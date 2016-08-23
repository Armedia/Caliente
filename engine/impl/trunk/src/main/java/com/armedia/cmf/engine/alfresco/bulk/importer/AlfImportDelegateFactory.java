package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.InvalidPropertiesFormatException;
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoSchema;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public class AlfImportDelegateFactory
	extends ImportDelegateFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine> {

	private static interface MappingValidator {
		public void validate(String key, String value) throws Exception;
	}

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

	private static boolean loadMap(final Logger log, String mapFile, Properties properties) {
		return AlfImportDelegateFactory.loadMap(log, mapFile, properties, null);
	}

	private static boolean loadMap(final Logger log, String mapFile, Properties properties,
		MappingValidator validator) {
		if (StringUtils.isEmpty(mapFile)) { return false; }

		File f = new File(mapFile);
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// Screw it...ignore the problem
			if (log.isDebugEnabled()) {
				log.warn(String.format("Failed to canonicalize the file path [%s]", mapFile), e);
			}
		}
		if (!f.exists()) {
			log.warn("The file [{}] does not exist", mapFile);
			return false;
		}
		if (!f.isFile()) {
			log.warn("The file [{}] is not a regular file", mapFile);
			return false;
		}
		if (!f.canRead()) {
			log.warn("The file [{}] is not readable", mapFile);
			return false;
		}

		Properties p = new Properties();
		try {
			InputStream in = new FileInputStream(f);
			try {
				// First, try the XML format
				p.clear();
				p.loadFromXML(in);
				for (Object k : p.keySet()) {
					String v = p.getProperty(k.toString());
					if (validator != null) {
						try {
							validator.validate(k.toString(), v);
						} catch (Exception e) {
							log.error(String.format("Mapping error detected in file [%s]: [%s]->[%s]", mapFile,
								k.toString(), v.toString()), e);
						}
					}
					properties.setProperty(k.toString(), v);
				}
				return true;
			} catch (InvalidPropertiesFormatException e) {
				// Not XML-format, try text format
				IOUtils.closeQuietly(in);
				in = new FileInputStream(f);

				p.clear();
				p.load(in);
				for (Object k : p.keySet()) {
					String v = p.getProperty(k.toString());
					if (validator != null) {
						try {
							validator.validate(k.toString(), v);
						} catch (Exception e2) {
							log.error(String.format("Mapping error detected in file [%s]: [%s]->[%s]", mapFile,
								k.toString(), v.toString()), e2);
						}
					}
					properties.setProperty(k.toString(), v);
				}
			} finally {
				IOUtils.closeQuietly(in);
			}
		} catch (IOException e) {
			log.warn(String.format("Failed to load the properties from file [%s]", mapFile), e);
			p.clear();
		}
		return false;
	}

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
		AlfImportDelegateFactory.loadMap(this.log, configuration.getString(AlfSessionFactory.TYPE_MAP), this.typeMap,
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
		AlfImportDelegateFactory.loadMap(this.log, configuration.getString(AlfSessionFactory.USER_MAP), this.userMap);
		AlfImportDelegateFactory.loadMap(this.log, configuration.getString(AlfSessionFactory.GROUP_MAP), this.groupMap);
		AlfImportDelegateFactory.loadMap(this.log, configuration.getString(AlfSessionFactory.ROLE_MAP), this.roleMap);
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