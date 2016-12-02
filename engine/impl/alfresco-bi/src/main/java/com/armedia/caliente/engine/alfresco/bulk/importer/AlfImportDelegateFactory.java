package com.armedia.caliente.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.caliente.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.caliente.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.caliente.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.caliente.engine.alfresco.bulk.common.AlfrescoBaseBulkOrganizationStrategy;
import com.armedia.caliente.engine.alfresco.bulk.importer.cache.Cache;
import com.armedia.caliente.engine.alfresco.bulk.importer.cache.CacheItem;
import com.armedia.caliente.engine.alfresco.bulk.importer.cache.CacheItemMarker;
import com.armedia.caliente.engine.alfresco.bulk.importer.cache.CacheItemMarker.MarkerType;
import com.armedia.caliente.engine.alfresco.bulk.importer.cache.CacheItemVersion;
import com.armedia.caliente.engine.alfresco.bulk.importer.model.AlfrescoSchema;
import com.armedia.caliente.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.tools.MappingTools;
import com.armedia.caliente.engine.tools.MappingTools.MappingValidator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public class AlfImportDelegateFactory
	extends ImportDelegateFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine> {

	private final class VirtualDocument {
		private final String historyId;
		private CacheItemMarker root = null;
		private final Map<String, CacheItemMarker> versions = new TreeMap<>();
		private final Map<String, List<CacheItemMarker>> members = new TreeMap<>();

		private VirtualDocument(String historyId) {
			this.historyId = historyId;
		}

		public void setRoot(CacheItemMarker marker) throws ImportException {
			if (this.root != null) { throw new ImportException("This virtual document already has a root element"); }
			this.root = marker;
		}

		public void addVersion(CacheItemMarker version) throws ImportException {
			if (this.versions.containsKey(version.getName())) { throw new ImportException(
				String.format("This virtual document already has a version called [%s]", version.getName())); }
			this.versions.put(version.getName(), version);
			this.members.put(version.getName(), new ArrayList<CacheItemMarker>());
		}

		public void addMember(String version, CacheItemMarker member) throws ImportException {
			List<CacheItemMarker> markers = this.members.get(version);
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
					for (CacheItemMarker marker : this.members.get(v)) {
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

	private static final Pattern TYPE_MAPPING_PARSER = Pattern.compile("^([^\\[]+)(?:\\[(.*)\\])?$");

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

	private final Properties typeMap = new Properties();
	private final Properties userMap = new Properties();
	private final Properties userLoginMap = new Properties();
	private final Properties groupMap = new Properties();
	private final Properties roleMap = new Properties();

	protected final AlfrescoSchema schema;
	private final Map<String, AlfrescoType> defaultTypes;
	private final Map<String, AlfrescoType> mappedTypes;

	private final ConcurrentMap<String, VirtualDocument> vdocs = new ConcurrentHashMap<>();
	private final ThreadLocal<List<CacheItemMarker>> currentVersions = new ThreadLocal<>();

	private final AlfXmlIndex fileIndex;
	private final AlfXmlIndex folderIndex;
	private final AtomicBoolean manifestSerialized = new AtomicBoolean(false);

	public AlfImportDelegateFactory(AlfImportEngine engine, CfgTools configuration)
		throws IOException, JAXBException, XMLStreamException {
		super(engine, configuration);
		String db = configuration.getString(AlfSessionFactory.DB);
		if (db != null) {
			this.db = new File(db).getCanonicalFile();
		} else {
			this.db = new File("cmsmf-xml").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.db);
		String content = configuration.getString(AlfSessionFactory.CONTENT);
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
			Cache.class, CacheItem.class, CacheItemVersion.class
		};
		this.fileIndex = new AlfXmlIndex(new File(biRootFile, AlfImportDelegateFactory.FILE_CACHE_FILE), idxClasses);
		this.folderIndex = new AlfXmlIndex(new File(biRootFile, AlfImportDelegateFactory.FOLDER_CACHE_FILE),
			idxClasses);

		String contentModels = configuration.getString(AlfSessionFactory.CONTENT_MODEL);
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

		final Map<String, AlfrescoType> mappedTypes = new TreeMap<>();
		MappingTools.loadMap(this.log, configuration.getString(AlfSessionFactory.TYPE_MAP), this.typeMap,
			new MappingValidator() {

				@Override
				public void validate(String key, String value) throws Exception {
					if (StringUtils.isEmpty(
						key)) { throw new Exception(String.format("No key provided when mapping for [%s]", value)); }
					if (StringUtils
						.isEmpty(value)) { throw new Exception(String.format("No mapping for type [%s]", key)); }
					Matcher m = AlfImportDelegateFactory.TYPE_MAPPING_PARSER.matcher(value);
					if (!m.matches()) { throw new Exception(
						String.format("Mapping does not conform to the required syntax")); }
					// All is well, move on...
					String baseName = m.group(1);
					if (!AlfImportDelegateFactory.this.schema.hasType(baseName)) { throw new Exception(
						String.format("No default type named [%s] was found", baseName)); }

					String aspects = m.group(2);
					if (!StringUtils.isEmpty(aspects)) {
						List<String> l = new ArrayList<>();
						for (String aspect : aspects.split(",")) {
							aspect = aspect.trim();
							if (StringUtils.isEmpty(aspects)) {
								continue;
							}
							if (!AlfImportDelegateFactory.this.schema.hasAspect(aspect)) { throw new Exception(
								String.format("No aspect named [%s] was found", baseName)); }
							l.add(aspect);
						}
						// Build with aspects...
						mappedTypes.put(key, AlfImportDelegateFactory.this.schema.buildType(baseName, l));
					} else {
						// No need to build since we lack aspects...
						mappedTypes.put(key, AlfImportDelegateFactory.this.defaultTypes.get(baseName));
					}
				}
			});
		if (mappedTypes.isEmpty()) {
			this.log.warn("No type mappings defined, only the default fallback types and aspects will be used");
		}
		this.mappedTypes = Tools.freezeMap(mappedTypes);
		MappingTools.loadMap(this.log, configuration.getString(AlfSessionFactory.USER_MAP), this.userMap);
		MappingTools.loadMap(this.log, configuration.getString(AlfSessionFactory.GROUP_MAP), this.groupMap);
		MappingTools.loadMap(this.log, configuration.getString(AlfSessionFactory.ROLE_MAP), this.roleMap);
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

		CacheItemMarker thisMarker = new CacheItemMarker();
		thisMarker.setDirectory(false);
		thisMarker.setName(name);
		thisMarker.setContent(relativeContentPath);
		thisMarker.setMetadata(null);
		thisMarker.setLocalPath(Paths.get(""));

		thisMarker.setNumber(AlfImportDelegateFactory.LAST_INDEX);
		thisMarker.setCmsPath("");

		List<CacheItemMarker> markerList = new ArrayList<>(1);
		markerList.add(thisMarker);

		final CacheItem item = thisMarker.getItem(markerList);
		try {
			this.fileIndex.marshal(item);
		} catch (Exception e) {
			throw new ImportException(String.format("Failed to serialize the file to XML: %s", item), e);
		}
	}

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

	protected final CacheItemMarker generateItemMarker(final AlfImportContext ctx, final CmfObject<CmfValue> cmfObject,
		File contentFile, File metadataFile, MarkerType type) throws ImportException {
		final boolean folder = type.isFolder(contentFile);
		final int head;
		final int count;
		final int current;
		switch (type) {
			case RENDITION_ROOT:
				contentFile = contentFile.getParentFile();
				// Fall-through
			case RENDITION_TYPE:
				contentFile = contentFile.getParentFile();
				// Fall-through
			case RENDITION_ENTRY:
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
						throw new ImportException(String.format("No version indexes found for (%s)[%s]",
							cmfObject.getLabel(), cmfObject.getId()));
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
		final Path relativeMetadataParent = (metadataFile != null ? relativeMetadataPath.getParent() : null);

		CacheItemMarker thisMarker = new CacheItemMarker();
		thisMarker.setDirectory(folder);
		thisMarker.setContent(relativeContentPath);
		thisMarker.setMetadata(relativeMetadataPath);
		thisMarker.setLocalPath(relativeMetadataParent != null ? relativeMetadataParent : Paths.get(""));

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

		CmfProperty<CmfValue> cmsPathProp = cmfObject.getProperty(IntermediateProperty.PATH);
		String cmsPath = ((cmsPathProp == null) || !cmsPathProp.hasValues() ? "" : cmsPathProp.getValue().asString());
		// Remove the leading slash(es)
		while (cmsPath.startsWith("/")) {
			cmsPath = cmsPath.substring(1);
		}

		cmsPath = resolveFixedNames(ctx, cmsPath, relativeMetadataParent);

		String append = null;
		// This is the base name, others may change it...
		thisMarker.setName(contentFile.getName());
		switch (type) {
			case NORMAL:
			case VDOC_ROOT:
				thisMarker.setName(cmfObject.getName());
				break;

			case VDOC_RENDITION:
				"".hashCode();
				// fall-through
			case VDOC_STREAM:
				// For the primary streams, we set the same name of the object
				thisMarker.setName(cmfObject.getName());
				// fall-through
			case VDOC_REFERENCE:
				// For the member, we have to append one more item to the cmsPath
				append = contentFile.getParentFile().getName();
				// fall-through
			case VDOC_VERSION:
				cmsPath = String.format("%s/%s", cmsPath, cmfObject.getName());
				if (append != null) {
					cmsPath = String.format("%s/%s", cmsPath, append);
				}
				break;

			case RENDITION_ENTRY:
			case RENDITION_TYPE:
				cmsPathProp = cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
				String specialCmsPath = ((cmsPathProp == null) || !cmsPathProp.hasValues() ? ""
					: cmsPathProp.getValue().asString());
				Path p = Paths.get(specialCmsPath);
				p = p.relativize(relativeContentPath);
				cmsPath = String.format("%s/%s", cmsPath, p.getParent().toString());
				break;

			default:
				break;
		}
		thisMarker.setCmsPath(cmsPath);
		thisMarker.setIndex(current);
		thisMarker.setHeadIndex(head);
		thisMarker.setVersionCount(count);
		return thisMarker;
	}

	private final void handleVirtual(final CmfObject<CmfValue> cmfObject, File contentFile, File metadataFile,
		MarkerType type, CacheItemMarker thisMarker) throws ImportException {
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

	protected final void storeToIndex(final AlfImportContext ctx, final CmfObject<CmfValue> cmfObject, File contentFile,
		File metadataFile, MarkerType type) throws ImportException {

		storeIngestionIndexToScanIndex();

		final CacheItemMarker thisMarker = generateItemMarker(ctx, cmfObject, contentFile, metadataFile, type);
		List<CacheItemMarker> markerList = null;
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

		CacheItemMarker headMarker = thisMarker;
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

		CacheItem item = headMarker.getItem(markerList);
		markerList.clear();

		try {
			(folder ? this.folderIndex : this.fileIndex).marshal(item);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to serialize the %s to XML: %s", folder ? "folder" : "file", item), e);
		}
	}

	public final AlfrescoType mapType(String type) {
		return this.mappedTypes.get(type);
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
		this.folderIndex.close();
		super.close();
	}
}