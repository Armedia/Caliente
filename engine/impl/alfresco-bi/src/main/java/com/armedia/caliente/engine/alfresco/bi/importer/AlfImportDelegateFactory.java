package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.caliente.engine.alfresco.bi.AlfCommon;
import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.alfresco.bi.AlfSessionWrapper;
import com.armedia.caliente.engine.alfresco.bi.AlfSetting;
import com.armedia.caliente.engine.alfresco.bi.AlfXmlIndex;
import com.armedia.caliente.engine.alfresco.bi.AlfrescoBaseBulkOrganizationStrategy;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.index.ScanIndex;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.index.ScanIndexItem;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.index.ScanIndexItemMarker;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.index.ScanIndexItemMarker.MarkerType;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.index.ScanIndexItemVersion;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.PrincipalMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.mapper.AttributeMapper;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoSchema;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dynamic.DynamicElementException;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.tools.MappingTools;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public class AlfImportDelegateFactory
	extends ImportDelegateFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine> {

	static final String METADATA_SUFFIX = ".metadata.properties.xml";

	private final class VirtualDocument {
		private final String historyId;
		private ScanIndexItemMarker root = null;
		private final Map<String, ScanIndexItemMarker> versions = new TreeMap<>();
		private final Map<String, List<ScanIndexItemMarker>> members = new TreeMap<>();

		private VirtualDocument(String historyId) {
			this.historyId = historyId;
		}

		public void setRoot(ScanIndexItemMarker marker) throws ImportException {
			if (this.root != null) { throw new ImportException("This virtual document already has a root element"); }
			this.root = marker;
		}

		public void addVersion(ScanIndexItemMarker version) throws ImportException {
			if (this.versions.containsKey(version.getTargetName())) { throw new ImportException(
				String.format("This virtual document already has a version called [%s]", version.getTargetName())); }
			this.versions.put(version.getTargetName(), version);
			this.members.put(version.getTargetName(), new ArrayList<ScanIndexItemMarker>());
		}

		public void addMember(String version, ScanIndexItemMarker member) throws ImportException {
			List<ScanIndexItemMarker> markers = this.members.get(version);
			if ((markers == null) || !this.versions.containsKey(version)) { throw new ImportException(
				String.format("This virtual document has no version called [%s]", version)); }
			markers.add(member);
		}

		public void serialize() throws Exception {
			if (!isComplete()) { throw new Exception(
				"This virtual document is not complete - can't serialize it yet"); }

			synchronized (AlfImportDelegateFactory.this.folderIndex) {
				AlfImportDelegateFactory.this.folderIndex.marshal(this.root.getItem());
				for (String v : this.versions.keySet()) {
					AlfImportDelegateFactory.this.folderIndex.marshal(this.versions.get(v).getItem());
				}
			}

			synchronized (AlfImportDelegateFactory.this.fileIndex) {
				for (String v : this.members.keySet()) {
					for (ScanIndexItemMarker marker : this.members.get(v)) {
						(marker.isDirectory() ? AlfImportDelegateFactory.this.folderIndex
							: AlfImportDelegateFactory.this.fileIndex).marshal(marker.getItem());
					}
				}
			}
			clear();
		}

		public void clear() {
			this.root = null;
			this.versions.clear();
			this.members.clear();
		}

		public boolean isComplete() {
			if (this.root == null) { return false; }
			if (this.versions.isEmpty()) { return false; }
			if (this.versions.size() != this.members.size()) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("VirtualDocument [historyId=%s, root=%s, versions=%s, members=%s]", this.historyId,
				this.root, this.versions, this.members);
		}
	}

	private final static String FILE_CACHE_FILE = "scan.files.xml";
	private final static String FOLDER_CACHE_FILE = "scan.folders.xml";

	private static final Pattern VERSION_SUFFIX = Pattern.compile("^.*(\\.v(\\d+(?:\\.\\d+)?))$");

	private static final BigDecimal LAST_INDEX = new BigDecimal(Long.MAX_VALUE);

	private static final String SCHEMA_NAME = "alfresco-model.xsd";

	private static final String MODEL_DIR_NAME = "content-models";

	static final Schema SCHEMA;

	static {
		try {
			SCHEMA = XmlTools.loadSchema(AlfImportDelegateFactory.SCHEMA_NAME);
		} catch (JAXBException e) {
			throw new RuntimeException(String.format("Failed to load the required schema resource [%s]",
				AlfImportDelegateFactory.SCHEMA_NAME));
		}
	}

	private final File db;
	private final File content;
	private final Path biRootPath;

	private final Properties userMap = new Properties();
	private final Properties userLoginMap = new Properties();
	private final Properties groupMap = new Properties();
	private final Properties roleMap = new Properties();
	private final AttributeMapper attributeMapper;

	private final Set<String> userAttributes;
	private final Set<String> groupAttributes;
	private final Set<String> roleAttributes;

	protected final AlfrescoSchema schema;
	private final Map<String, AlfrescoType> defaultTypes;

	private final ConcurrentMap<String, VirtualDocument> vdocs = new ConcurrentHashMap<>();
	private final ThreadLocal<List<ScanIndexItemMarker>> currentVersions = new ThreadLocal<>();

	private final AlfXmlIndex fileIndex;
	private final AlfXmlIndex folderIndex;
	private final AtomicBoolean manifestSerialized = new AtomicBoolean(false);
	private final AtomicReference<Boolean> initializedVdocs = new AtomicReference<>(null);

	private final Set<String> artificialFolders = Collections.synchronizedSet(new LinkedHashSet<String>());

	public AlfImportDelegateFactory(AlfImportEngine engine, CfgTools configuration)
		throws IOException, JAXBException, XMLStreamException, DynamicElementException {
		super(engine, configuration);
		String db = configuration.getString(AlfSetting.DB);
		if (db != null) {
			this.db = new File(db).getCanonicalFile();
		} else {
			this.db = new File("cmsmf-xml").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.db);
		String content = configuration.getString(AlfSetting.CONTENT);
		if (content != null) {
			this.content = new File(content).getCanonicalFile();
		} else {
			this.content = new File(db, "content").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.content);
		final File modelDir = new File(this.content, AlfImportDelegateFactory.MODEL_DIR_NAME);
		FileUtils.forceMkdir(modelDir);

		final File biRootFile = new File(this.content, AlfrescoBaseBulkOrganizationStrategy.BASE_DIR);
		this.biRootPath = biRootFile.toPath();
		Class<?>[] idxClasses = {
			ScanIndex.class, ScanIndexItem.class, ScanIndexItemVersion.class
		};
		this.fileIndex = new AlfXmlIndex(new File(biRootFile, AlfImportDelegateFactory.FILE_CACHE_FILE), idxClasses);
		this.folderIndex = new AlfXmlIndex(new File(biRootFile, AlfImportDelegateFactory.FOLDER_CACHE_FILE),
			idxClasses);

		String contentModels = configuration.getString(AlfSetting.CONTENT_MODEL);
		if (contentModels == null) { throw new IllegalStateException(
			"Must provide a valid set of content model XML files"); }

		List<URI> modelUrls = new ArrayList<>();
		for (String s : contentModels.split(",")) {
			File f = new File(s).getCanonicalFile();
			if (!f.exists()) { throw new FileNotFoundException(f.getAbsolutePath()); }
			if (!f.isFile()) { throw new IOException(
				String.format("File [%s] is not a regular file", f.getAbsolutePath())); }
			if (!f
				.canRead()) { throw new IOException(String.format("File [%s] is not readable", f.getAbsolutePath())); }
			modelUrls.add(f.toURI());
			FileUtils.copyFile(f, new File(modelDir, f.getName()));
		}

		this.schema = new AlfrescoSchema(modelUrls);

		Map<String, AlfrescoType> m = new TreeMap<>();
		// First, we build all the base types, to have them cached and ready to go
		for (String t : this.schema.getTypeNames()) {
			m.put(t, this.schema.buildType(t));
		}

		this.defaultTypes = Tools.freezeMap(new LinkedHashMap<>(m));

		if (!MappingTools.loadMap(this.log, configuration, AlfSetting.USER_MAP, this.userMap)) {
			this.userMap.clear();
		}
		if (!MappingTools.loadMap(this.log, configuration, AlfSetting.GROUP_MAP, this.groupMap)) {
			this.groupMap.clear();
		}
		if (!MappingTools.loadMap(this.log, configuration, AlfSetting.ROLE_MAP, this.roleMap)) {
			this.roleMap.clear();
		}

		String pfx = configuration.getString(AlfSetting.RESIDUALS_PREFIX);
		pfx = StringUtils.strip(pfx);
		if (StringUtils.isEmpty(pfx)) {
			pfx = null;
		}
		this.attributeMapper = new AttributeMapper(this.schema, configuration.getString(AlfSetting.ATTRIBUTE_MAPPING),
			pfx);

		PrincipalMappings pmap = AttributeMapper.PRINCIPAL_MAPPINGS
			.getInstance(configuration.getString(AlfSetting.PRINCIPAL_MAPPING));
		if (pmap == null) {
			pmap = new PrincipalMappings();
		}
		this.userAttributes = Tools.freezeSet(new LinkedHashSet<>(pmap.getUserSet()));
		this.groupAttributes = Tools.freezeSet(new LinkedHashSet<>(pmap.getGroupSet()));
		this.roleAttributes = Tools.freezeSet(new LinkedHashSet<>(pmap.getRoleSet()));
	}

	public Set<String> getUserAttributes() {
		return this.userAttributes;
	}

	public Set<String> getGroupAttributes() {
		return this.groupAttributes;
	}

	public Set<String> getRoleAttributes() {
		return this.roleAttributes;
	}

	protected AlfrescoSchema getSchema() {
		return this.schema;
	}

	public AttributeMapper getAttributeMapper() {
		return this.attributeMapper;
	}

	protected AlfrescoType getType(String name, String... aspects) {
		if ((aspects == null) || (aspects.length == 0)) { return this.defaultTypes.get(name); }
		return this.schema.buildType(name, aspects);
	}

	protected AlfrescoType getType(String name, Collection<String> aspects) {
		if ((aspects == null) || aspects.isEmpty()) { return this.defaultTypes.get(name); }
		return this.schema.buildType(name, aspects);
	}

	String relativizeXmlLocation(String absolutePath) {
		String base = String.format("%s/", this.content.getAbsolutePath().replace(File.separatorChar, '/'));
		absolutePath = absolutePath.replace(File.separatorChar, '/');
		return absolutePath.substring(base.length());
	}

	File getLocation(String relativePath) {
		return new File(this.content, relativePath);
	}

	boolean initializeVdocSupport() {
		if (this.initializedVdocs.get() == null) {
			synchronized (this) {
				if (this.initializedVdocs.get() == null) {
					boolean ok = false;
					try {
						getType("cm:folder", "dctm:vdocRoot");
						getType("cm:folder", "dctm:vdocVersion");
						getType("dctm:vdocReference");
						ok = true;
					} catch (Exception e) {
						this.log.warn("VDoc support has been disabled", e);
					} finally {
						// Make sure we only do this once
						this.initializedVdocs.set(ok);
					}
				}
			}
		}
		return this.initializedVdocs.get();
	}

	@Override
	protected AlfImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case USER:
				return new AlfImportUserDelegate(this, storedObject);
			case FOLDER:
				return new AlfImportFolderDelegate(this, storedObject);
			case DOCUMENT:
				// TODO: How to determine the minor counter
				return new AlfImportDocumentDelegate(this, storedObject);
			default:
				break;
		}
		return null;
	}

	protected File calculateConsolidatedFile(CmfType t) {
		return new File(this.db, String.format("%ss.xml", t.name().toLowerCase()));
	}

	boolean mapUserLogin(String userName, String login) {
		// Only add the mapping if the username is different from the login
		if ((userName == null) || Tools.equals(userName, login)) { return false; }
		if (login == null) {
			this.userLoginMap.remove(userName);
		} else {
			this.userLoginMap.setProperty(userName, login);
		}
		return true;
	}

	static final File normalizeAbsolute(File f) {
		if (f == null) { return null; }
		f = f.getAbsoluteFile();
		f = new File(FilenameUtils.normalize(f.getAbsolutePath()));
		return f.getAbsoluteFile();
	}

	static final String parseVersionSuffix(String s) {
		final Matcher m = AlfImportDelegateFactory.VERSION_SUFFIX.matcher(s);
		if (!m.matches()) { return ""; }
		return m.group(1);
	}

	static final String parseVersionNumber(String s) {
		final Matcher m = AlfImportDelegateFactory.VERSION_SUFFIX.matcher(s);
		if (!m.matches()) { return null; }
		return m.group(2);
	}

	static final File removeVersionTag(File f) {
		return AlfImportDelegateFactory.removeVersionTag(f.toPath()).toFile();
	}

	static final Path removeVersionTag(Path p) {
		final String suffix = AlfImportDelegateFactory.parseVersionSuffix(p.toString());
		if (suffix == null) { return p; }
		Path parent = p.getParent();
		String name = p.getFileName().toString().replaceAll(String.format("\\Q%s\\E$", suffix), "");
		return (parent != null ? parent.resolve(name) : Paths.get(name));
	}

	private final void storeIngestionIndexToScanIndex() throws ImportException {
		if (!this.manifestSerialized.compareAndSet(false, true)) {
			// This will only happen once
			return;
		}

		final String name = AlfImportEngine.MANIFEST_NAME;
		final Path relativeContentPath = Paths.get(name);

		ScanIndexItemMarker thisMarker = new ScanIndexItemMarker();
		thisMarker.setDirectory(false);
		thisMarker.setContent(relativeContentPath);
		thisMarker.setMetadata(null);
		thisMarker.setSourcePath(Paths.get(""));
		thisMarker.setSourceName(name);
		thisMarker.setTargetPath("");
		thisMarker.setTargetName(name);
		thisMarker.setNumber(AlfImportDelegateFactory.LAST_INDEX);

		List<ScanIndexItemMarker> markerList = new ArrayList<>(1);
		markerList.add(thisMarker);

		final ScanIndexItem item = thisMarker.getItem(markerList);
		try {
			this.fileIndex.marshal(item);
		} catch (Exception e) {
			throw new ImportException(String.format("Failed to serialize the file to XML: %s", item), e);
		}
	}

	@SuppressWarnings("unused")
	private final String resolveFixedNames(final AlfImportContext ctx, String cmsPath, Path idPath)
		throws ImportException {
		if ((idPath != null) && (idPath.getNameCount() > 0)) {
			// First things first: tokenize each of them
			final int nameCount = idPath.getNameCount();
			List<String> names = new StrTokenizer(cmsPath, '/').getTokenList();
			if (names.size() > nameCount) {
				// WTF?!?
				throw new ImportException(String.format(
					"The CMS path [%s] has a more components than the ID path [%s] - this is an export error", cmsPath,
					idPath.toString()));
			}

			// We will only resolve however many components are present in the CMS path
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < names.size(); i++) {
				final String folderId = idPath.getName(i).toString();
				String folderName = names.get(i);
				// If we're still resolving valid IDs...
				final String altName = ctx.getAlternateName(CmfType.FOLDER, folderId);
				if ((altName != null) && !Tools.equals(altName, folderName)) {
					folderName = altName;
				}
				(i > 0 ? b.append('/') : b).append(folderName);
			}
			cmsPath = b.toString();
		}
		return cmsPath;
	}

	private final String resolveTreeIds(final AlfImportContext ctx, String cmsPath) throws ImportException {
		List<CmfObjectRef> refs = new ArrayList<>();
		for (String id : StringUtils.split(cmsPath, '/')) {
			// They're all known to be folders, so...
			refs.add(new CmfObjectRef(CmfType.FOLDER, id));
		}
		Map<CmfObjectRef, String> names = ctx.getObjectNames(refs, true);
		StringBuilder path = new StringBuilder();
		for (CmfObjectRef ref : refs) {
			final String name = names.get(ref);
			if (name == null) {
				// WTF?!?!?
				throw new ImportException(String.format("Failed to resolve the name for %s", ref));
			}

			if (StringUtils.isEmpty(name)) {
				continue;
			}
			if (path.length() > 0) {
				path.append('/');
			}
			path.append(name);
		}
		return path.toString();
	}

	protected final ScanIndexItemMarker generateItemMarker(final AlfImportContext ctx,
		final CmfObject<CmfValue> cmfObject, File contentFile, File metadataFile, MarkerType type)
		throws ImportException {
		final boolean folder = type.isFolder(contentFile);
		final int head;
		final int count;
		final int current;
		String renditionRootPath = null;
		switch (type) {
			case RENDITION_ROOT:
				contentFile = contentFile.getParentFile();
				// Fall-through
			case RENDITION_TYPE:
				contentFile = contentFile.getParentFile();
				// Fall-through
			case RENDITION_ENTRY:
				renditionRootPath = String.format("%s-renditions", cmfObject.getId());
				// Fall-through
			case VDOC_ROOT:
			case VDOC_VERSION:
			case VDOC_STREAM:
			case VDOC_RENDITION:
			case VDOC_REFERENCE:
				head = 1;
				count = 1;
				current = 1;
				break;

			case NORMAL:
			default:
				CmfProperty<CmfValue> vCounter = cmfObject.getProperty(IntermediateProperty.VERSION_COUNT);
				CmfProperty<CmfValue> vHeadIndex = cmfObject.getProperty(IntermediateProperty.VERSION_HEAD_INDEX);
				CmfProperty<CmfValue> vIndex = cmfObject.getProperty(IntermediateProperty.VERSION_INDEX);
				if ((vCounter == null) || !vCounter.hasValues() || //
					(vHeadIndex == null) || !vHeadIndex.hasValues() || //
					(vIndex == null) || !vIndex.hasValues()) {
					if (!folder) {
						// ERROR: insufficient data
						throw new ImportException(
							String.format("No version indexes found for %s", cmfObject.getDescription()));
					}
					// It's OK for directories...everything is 1
					head = 1;
					count = 1;
					current = 1;
				} else {
					head = vHeadIndex.getValue().asInteger();
					count = vCounter.getValue().asInteger();
					current = vIndex.getValue().asInteger();
				}
				break;
		}

		final boolean headVersion = (head == current);
		final boolean lastVersion = (count == current);

		// We don't use canonicalize() here because we want to be able to respect symlinks if
		// for whatever reason they need to be employed
		contentFile = AlfImportDelegateFactory.normalizeAbsolute(contentFile);
		metadataFile = AlfImportDelegateFactory.normalizeAbsolute(metadataFile);

		// basePath is the base path within which the entire import resides
		final Path relativeContentPath = this.biRootPath.relativize(contentFile.toPath());
		final Path relativeMetadataPath = (metadataFile != null ? this.biRootPath.relativize(metadataFile.toPath())
			: null);
		Path relativeMetadataParent = (metadataFile != null ? relativeMetadataPath.getParent() : null);
		if (relativeMetadataParent == null) {
			// if there's no metadata file, we fall back to the content file's parent...
			relativeMetadataParent = (relativeContentPath != null ? relativeContentPath.getParent() : null);
		}

		ScanIndexItemMarker thisMarker = new ScanIndexItemMarker();
		thisMarker.setDirectory(folder);
		thisMarker.setContent(relativeContentPath);
		thisMarker.setMetadata(relativeMetadataPath);
		thisMarker.setSourcePath(relativeMetadataParent != null ? relativeMetadataParent : Paths.get(""));
		thisMarker.setSourceName(contentFile.getName());

		BigDecimal number = AlfImportDelegateFactory.LAST_INDEX;
		if (!headVersion || !lastVersion) {
			// Parse out the version number, so we don't have to muck around with having
			// to guess which "type" is in use
			String n = AlfImportDelegateFactory.parseVersionNumber(contentFile.getName());
			if (n != null) {
				number = new BigDecimal(n);
			}
		}
		thisMarker.setNumber(number);

		CmfProperty<CmfValue> sourcePathProp = cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		String targetPath = ((sourcePathProp == null) || !sourcePathProp.hasValues() ? ""
			: sourcePathProp.getValue().asString());
		targetPath = resolveTreeIds(ctx, targetPath);

		final boolean unfiled;
		{
			CmfProperty<CmfValue> unfiledProp = cmfObject.getProperty(IntermediateProperty.IS_UNFILED);
			unfiled = (unfiledProp != null) && unfiledProp.hasValues() && unfiledProp.getValue().asBoolean();
		}

		if (unfiled) {
			String balancedPath = String.format("%08x", cmfObject.getNumber());
			// Split the number every 2 digits..., but only the first 6
			balancedPath = balancedPath.substring(0, 6);
			List<String> l = new ArrayList<>(3);
			for (int i = 0; i < 3; i++) {
				l.add(balancedPath.substring(i * 2, (i * 2) + 2));
			}
			targetPath = String.format("(unfiled)/%s", Tools.joinEscaped('/', l));
			storeArtificialFolderToIndex(targetPath);
		}

		String append = null;
		// This is the base name, others may change it...
		thisMarker.setTargetName(contentFile.getName());
		switch (type) {
			case NORMAL:
			case VDOC_ROOT:
				thisMarker.setTargetName(ctx.getObjectName(cmfObject));
				break;

			case VDOC_RENDITION:
				// fall-through
			case VDOC_STREAM:
				// For the primary streams, we set the same name of the object
				thisMarker.setTargetName(ctx.getObjectName(cmfObject));
				// fall-through
			case VDOC_REFERENCE:
				// For the member, we have to append one more item to the cmsPath
				append = contentFile.getParentFile().getName();
				// fall-through
			case VDOC_VERSION:
				targetPath = String.format("%s/%s", targetPath, ctx.getObjectName(cmfObject));
				if (append != null) {
					targetPath = String.format("%s/%s", targetPath, append);
				}
				break;

			case RENDITION_ROOT:
				// Special case: the target name must be ${objectId}-renditions
				thisMarker.setTargetName(String.format("%s-renditions", cmfObject.getId()));
				break;

			case RENDITION_TYPE:
				targetPath = String.format("%s/%s", targetPath, renditionRootPath);
				break;

			case RENDITION_ENTRY:
				// Add the rendition root path
				targetPath = String.format("%s/%s", targetPath, renditionRootPath);
				// Add the rendition type path
				targetPath = String.format("%s/%s", targetPath, contentFile.getParentFile().getName());
				break;

			default:
				break;
		}

		if (unfiled) {
			thisMarker.setTargetName(String.format("%08x-%s", cmfObject.getNumber(), thisMarker.getTargetName()));
		}
		thisMarker.setTargetPath(targetPath);
		thisMarker.setIndex(current);
		thisMarker.setHeadIndex(head);
		thisMarker.setVersionCount(count);
		return thisMarker;
	}

	private final void handleVirtual(final CmfObject<CmfValue> cmfObject, File contentFile, File metadataFile,
		MarkerType type, ScanIndexItemMarker thisMarker) throws ImportException {
		VirtualDocument vdoc = ConcurrentUtils.createIfAbsentUnchecked(this.vdocs, cmfObject.getHistoryId(),
			new ConcurrentInitializer<VirtualDocument>() {
				@Override
				public VirtualDocument get() throws ConcurrentException {
					return new VirtualDocument(cmfObject.getHistoryId());
				}
			});

		switch (type) {
			case VDOC_ROOT:
				vdoc.setRoot(thisMarker);
				break;

			case VDOC_VERSION:
				vdoc.addVersion(thisMarker);
				break;

			case VDOC_RENDITION:
			case VDOC_STREAM:
			case VDOC_REFERENCE:
				vdoc.addMember(contentFile.getParentFile().getName(), thisMarker);
				break;

			default:
				break;
		}
	}

	final File generateMetadataFile(final Properties p, final CmfObject<CmfValue> cmfObject, final File main)
		throws ImportException {
		String mainName = main.getName();
		final String suffix = AlfImportDelegateFactory.parseVersionSuffix(mainName);
		mainName = mainName.substring(0, mainName.length() - suffix.length());
		final File meta = new File(main.getParentFile(),
			String.format("%s%s%s", mainName, AlfImportDelegateFactory.METADATA_SUFFIX, suffix));
		if (p == null) { return meta; }

		final OutputStream out;
		try {
			out = new FileOutputStream(meta);
		} catch (FileNotFoundException e) {
			throw new ImportException(
				String.format("Failed to open the properties file at [%s]", main.getAbsolutePath()), e);
		}
		try {
			XmlProperties.saveToXML(p, out, String.format("Properties for %s", cmfObject.getDescription()));
		} catch (IOException e) {
			meta.delete();
			throw new ImportException(
				String.format("Failed to write to the properties file at [%s]", main.getAbsolutePath()), e);
		} finally {
			IOUtils.closeQuietly(out);
		}
		return meta;
	}

	protected final void storeArtificialFolderToIndex(String artificialFolder) throws ImportException {
		artificialFolder = FilenameUtils.normalize(artificialFolder, true);
		if (StringUtils.isEmpty(artificialFolder)) { return; }
		while (!Tools.equals(".", artificialFolder)) {
			this.artificialFolders.add(artificialFolder);
			artificialFolder = FileNameTools.dirname(artificialFolder, '/');
		}
	}

	private final void serializeArtificialFolders() throws XMLStreamException {
		if (this.artificialFolders.isEmpty()) { return; }
		this.folderIndex.writeComment(" Begin the artificial folders for unfiled objects ");
		final AtomicLong number = new AtomicLong(0);
		// Make sure we order them so they are sorted hierarchically
		for (final String mf : new TreeSet<>(this.artificialFolders)) {
			// Already processed - move along!
			final File baseFile;
			{
				List<String> paths = new ArrayList<>();
				String name = AlfCommon.addNumericPaths(paths, number.getAndIncrement());
				name = String.format("folder-patch.%s", name);
				// Concatenate them into a path
				String path = FileNameTools.reconstitute(paths, false, false);
				// Now, create a file for them
				Path basePath = this.biRootPath.resolve(path);
				baseFile = AlfImportDelegateFactory.normalizeAbsolute(basePath.resolve(name).toFile());
			}

			// Make sure nothing is there...
			if (baseFile.exists()) {
				try {
					FileUtils.forceDelete(baseFile);
				} catch (IOException e) {
					this.log.warn("Failed to delete the artificial folder [{}]", baseFile.getAbsolutePath(), e);
				}
			}

			// Generate the numeric base paths...

			final boolean root = (mf.indexOf('/') < 0);

			final String parent;
			final String name;
			if (root) {
				parent = "";
				name = mf;
			} else {
				parent = FileNameTools.dirname(mf, '/');
				name = FileNameTools.basename(mf, '/');
			}

			final ScanIndexItemMarker thisMarker = generateMissingFolderMarker(parent, name, baseFile);
			List<ScanIndexItemMarker> markerList = new ArrayList<>();
			markerList.add(thisMarker);

			ScanIndexItem item = thisMarker.getItem(markerList);
			markerList.clear();

			try {
				FileUtils.forceMkdir(baseFile);
				this.folderIndex.marshal(item);
			} catch (Exception e) {
				this.log.warn("Failed to create/serialize the artificial FOLDER to XML: {}", item, e);
			}
		}
		this.folderIndex.writeComment("  End the artificial folders for unfiled objects  ");
	}

	protected final ScanIndexItemMarker generateMissingFolderMarker(final String targetPath, final String folderName,
		File contentFile) {
		// We don't use canonicalize() here because we want to be able to respect symlinks if
		// for whatever reason they need to be employed
		contentFile = AlfImportDelegateFactory.normalizeAbsolute(contentFile);
		// basePath is the base path within which the entire import resides
		final Path relativeContentPath = this.biRootPath.relativize(contentFile.toPath());
		final Path relativeContentParent = (relativeContentPath != null ? relativeContentPath.getParent() : null);

		ScanIndexItemMarker thisMarker = new ScanIndexItemMarker();
		thisMarker.setDirectory(true);
		thisMarker.setContent(relativeContentPath);
		thisMarker.setSourcePath(relativeContentParent != null ? relativeContentParent : Paths.get(""));
		thisMarker.setSourceName(contentFile.getName());
		thisMarker.setNumber(AlfImportDelegateFactory.LAST_INDEX);
		thisMarker.setTargetName(folderName);
		thisMarker.setTargetPath(targetPath);
		thisMarker.setIndex(1);
		thisMarker.setHeadIndex(1);
		thisMarker.setVersionCount(1);
		return thisMarker;
	}

	protected final void storeToIndex(final AlfImportContext ctx, final CmfObject<CmfValue> cmfObject, File contentFile,
		File metadataFile, MarkerType type) throws ImportException {

		storeIngestionIndexToScanIndex();

		final ScanIndexItemMarker thisMarker = generateItemMarker(ctx, cmfObject, contentFile, metadataFile, type);
		List<ScanIndexItemMarker> markerList = null;
		switch (type) {
			case VDOC_ROOT:
			case VDOC_VERSION:
			case VDOC_STREAM:
			case VDOC_RENDITION:
			case VDOC_REFERENCE:
				handleVirtual(cmfObject, contentFile, metadataFile, type, thisMarker);
				return;

			case RENDITION_ROOT:
			case RENDITION_TYPE:
			case RENDITION_ENTRY:
				markerList = new ArrayList<>(1);
				break;

			case NORMAL:
				markerList = this.currentVersions.get();
				if (markerList == null) {
					markerList = new ArrayList<>();
					this.currentVersions.set(markerList);
				}
				break;

			default:
				break;
		}

		markerList.add(thisMarker);

		final boolean folder = thisMarker.isDirectory();

		if (!thisMarker.isLastVersion()) {
			// more versions to come, so we simply keep going...
			// can't output the XML element just yet...
			return;
		}

		ScanIndexItemMarker headMarker = thisMarker;
		if (!thisMarker.isHeadVersion()) {
			// This is not the head version. We need to make a copy
			// of it and change the version number...
			headMarker = markerList.get(thisMarker.getHeadIndex() - 1);
			headMarker = headMarker.clone();
			headMarker.setNumber(AlfImportDelegateFactory.LAST_INDEX);
			headMarker.setContent(AlfImportDelegateFactory.removeVersionTag(headMarker.getContent()));
			headMarker.setMetadata(AlfImportDelegateFactory.removeVersionTag(headMarker.getMetadata()));
			markerList.add(headMarker);
		}

		ScanIndexItem item = headMarker.getItem(markerList);
		markerList.clear();

		try {
			(folder ? this.folderIndex : this.fileIndex).marshal(item);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to serialize the %s to XML: %s", folder ? "folder" : "file", item), e);
		}
	}

	protected String mapUser(String user) {
		if (user == null) { return null; }
		user = Tools.coalesce(this.userLoginMap.getProperty(user), user);
		return Tools.coalesce(this.userMap.getProperty(user), user);
	}

	protected String mapGroup(String group) {
		if (group == null) { return null; }
		return Tools.coalesce(this.groupMap.getProperty(group), group);
	}

	protected String mapRole(String role) {
		if (role == null) { return null; }
		return Tools.coalesce(this.roleMap.getProperty(role), role);
	}

	@Override
	public void close() {
		for (VirtualDocument vdoc : this.vdocs.values()) {
			try {
				vdoc.serialize();
			} catch (Exception e) {
				// This should never happen, but we still look out for it
				this.log.warn(String.format("Failed to marshal the VDoc XML for [%s]", vdoc), e);
			}
		}

		this.fileIndex.close();
		// Make sure we do this last
		try {
			serializeArtificialFolders();
		} catch (Exception e) {
			this.log.warn("Failed to serialize the artificial folders", e);
		}
		this.folderIndex.close();
		super.close();
	}
}