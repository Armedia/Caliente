package com.armedia.cmf.engine.alfresco.bulk.importer;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfrescoBaseBulkOrganizationStrategy;
import com.armedia.cmf.engine.alfresco.bulk.importer.cache.CacheItem;
import com.armedia.cmf.engine.alfresco.bulk.importer.cache.CacheItemMarker;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoSchema;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.tools.MappingTools;
import com.armedia.cmf.engine.tools.MappingTools.MappingValidator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public class AlfImportDelegateFactory
	extends ImportDelegateFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine> {

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

	private final Properties typeMap = new Properties();
	private final Properties userMap = new Properties();
	private final Properties userLoginMap = new Properties();
	private final Properties groupMap = new Properties();
	private final Properties roleMap = new Properties();

	protected final AlfrescoSchema schema;
	private final Map<String, AlfrescoType> defaultTypes;
	private final Map<String, AlfrescoType> mappedTypes;

	private final ThreadLocal<List<CacheItemMarker>> currentVersions = new ThreadLocal<List<CacheItemMarker>>();

	public AlfImportDelegateFactory(AlfImportEngine engine, CfgTools configuration) throws IOException, JAXBException {
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

		String contentModels = configuration.getString(AlfSessionFactory.CONTENT_MODEL);
		if (contentModels == null) { throw new IllegalStateException(
			"Must provide a valid set of content model XML files"); }

		List<URI> modelUrls = new ArrayList<URI>();
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

		Map<String, AlfrescoType> m = new TreeMap<String, AlfrescoType>();
		// First, we build all the base types, to have them cached and ready to go
		for (String t : this.schema.getTypeNames()) {
			m.put(t, this.schema.buildType(t));
		}

		this.defaultTypes = Tools.freezeMap(new LinkedHashMap<String, AlfrescoType>(m));

		final Map<String, AlfrescoType> mappedTypes = new TreeMap<String, AlfrescoType>();
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
						List<String> l = new ArrayList<String>();
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

	final File normalizeAbsolute(File f) {
		f = f.getAbsoluteFile();
		f = new File(FilenameUtils.normalize(f.getAbsolutePath()));
		return f.getAbsoluteFile();
	}

	protected final void storeToIndex(final CmfObject<CmfValue> cmfObject, File root, File contentFile,
		File metadataFile) throws ImportException {

		List<CacheItemMarker> markerList = this.currentVersions.get();
		if (markerList == null) {
			markerList = new ArrayList<CacheItemMarker>();
			this.currentVersions.set(markerList);
		}

		CmfProperty<CmfValue> vCounter = cmfObject.getProperty(IntermediateProperty.VERSION_COUNT);
		CmfProperty<CmfValue> vHeadIndex = cmfObject.getProperty(IntermediateProperty.VERSION_HEAD_INDEX);
		CmfProperty<CmfValue> vIndex = cmfObject.getProperty(IntermediateProperty.VERSION_INDEX);

		final boolean directory = contentFile.isDirectory();
		final int head;
		final int count;
		final long current;

		if ((vCounter == null) || !vCounter.hasValues() || //
			(vHeadIndex == null) || !vHeadIndex.hasValues() || //
			(vIndex == null) || !vIndex.hasValues()) {
			if (!directory) {
				// ERROR: insufficient data
				throw new ImportException(
					String.format("No version indexes found for (%s)[%s]", cmfObject.getLabel(), cmfObject.getId()));
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

		if (current == 1) {
			// Paranoid...just make sure ;)
			markerList.clear();
		}

		final boolean isHeadVersion = (head == current);
		final boolean isLastVersion = (count == current);

		// We don't use canonicalize() here because we want to be able to respect symlinks if
		// for whatever reason they need to be employed
		root = new File(normalizeAbsolute(root), AlfrescoBaseBulkOrganizationStrategy.BASE_DIR);
		contentFile = normalizeAbsolute(contentFile);
		metadataFile = normalizeAbsolute(metadataFile);

		// basePath is the base path within which the entire import resides
		final Path basePath = root.toPath();

		final Path relativeContentPath = basePath.relativize(contentFile.toPath());
		final Path relativeMetadataPath = basePath.relativize(metadataFile.toPath());
		final Path relativeMetadataParent = relativeMetadataPath.getParent();

		CacheItemMarker thisMarker = new CacheItemMarker();
		thisMarker.setDirectory(directory);
		thisMarker.setName(cmfObject.getName());
		thisMarker.setContent(relativeContentPath);
		thisMarker.setMetadata(relativeMetadataPath);
		thisMarker.setLocalPath(relativeMetadataParent != null ? relativeMetadataParent : Paths.get(""));

		BigDecimal number = AlfImportDelegateFactory.LAST_INDEX;
		if (!isHeadVersion || !isLastVersion) {
			number = new BigDecimal(current);
		}
		// TODO: Do 0.XX or just XX?
		thisMarker.setNumber(number);

		CmfProperty<CmfValue> cmsPathProp = cmfObject.getProperty(IntermediateProperty.PATH);
		String cmsPath = ((cmsPathProp == null) || !cmsPathProp.hasValues() ? "" : cmsPathProp.getValue().asString());
		// Remove the leading slash(es)
		while (cmsPath.startsWith("/")) {
			cmsPath = cmsPath.substring(1);
		}
		thisMarker.setCmsPath(cmsPath);

		markerList.add(thisMarker);

		if (!isLastVersion) {
			// more versions to come, so we simply keep going...
			// can't output the XML element just yet...
			return;
		}

		CacheItemMarker headMarker = thisMarker;
		if (!isHeadVersion) {
			// This is not the head version. We need to make a copy
			// of it and change the version number...
			headMarker = markerList.get(head - 1);
			headMarker = headMarker.clone();
			headMarker.setNumber(AlfImportDelegateFactory.LAST_INDEX);
			markerList.add(headMarker);
			// TODO: Remove the version suffixes
		}

		CacheItem item = headMarker.getItem(markerList);
		CacheItem item2 = headMarker.getItem(markerList);
		markerList.clear();
		item.hashCode();
		item2.hashCode();

		// TODO: Write out the XML

		// If this is the last version, then output the XML...
		/*
		final Marshaller m = JAXBContext.newInstance(CacheItem.class, CacheItemVersion.class).createMarshaller();
		m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		final XMLStreamWriter xml = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(System.out));
		xml.writeStartDocument("UTF-8", "1.0");
		xml.writeStartElement("scan");
		xml.flush();
		
		for (Object xml : xmlObjects) {
			m.marshal(cacheItem, xml);
			xml.flush();
		}
		
		xml.writeEndDocument();
		xml.flush();
		 */
		/*
		<?xml version="1.0" encoding="UTF-8"?>
		<scan>
			<item>
				<directory>true</directory>
				<name>name</name>
				<fsRelativePath>fsRelativePathInTheFS</fsRelativePath>
				<relativePath>relativePathOnTheCMS</relativePath>
				<versions>
					<version>
						<number>1.0</number>
						<content>contentFile</content>
						<metadata>metadataFile</metadata>
					</version>
					<!-- version... -->
				</versions>
			</item>
			<item>
				<directory>false</directory>
				<name>name</name>
				<fsRelativePath>fsRelativePathInTheFS</fsRelativePath>
				<relativePath>relativePathOnTheCMS</relativePath>
				<versions>
					<version>
						<number>1.0</number>
						<content>contentFile</content>
						<metadata>metadataFile</metadata>
					</version>
					<!-- version... -->
				</versions>
			</item>
		</scan>
		*/
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
		super.close();
	}
}