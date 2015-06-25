package com.armedia.cmf.engine.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.mapper.xml.BaseTypeT;
import com.armedia.cmf.engine.mapper.xml.MappingsT;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfTypeMapperFactory;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.ConfigurationSetting;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public class FileBasedTypeMapperFactory extends CmfTypeMapperFactory {

	private static final String CLASSPATH = "classpath";

	public static enum Setting implements ConfigurationSetting {
		//
		MAPPING_FILE("type-mappings.xml"),
		//
		;

		private final Object defaultValue;
		private final String label;

		private Setting() {
			this(null);
		}

		private Setting(Object defaultValue) {
			this.defaultValue = defaultValue;
			this.label = name().replace('_', '.').toLowerCase();
		}

		@Override
		public String getLabel() {
			return this.label;
		}

		@Override
		public Object getDefaultValue() {
			return this.defaultValue;
		}

	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String SCHEMA_NAME = "type-mappings.xsd";

	private class Mapper extends CmfTypeMapper {

		private final Map<CmfType, Map<String, String>> mappings;

		private Mapper(MappingsT mappings) throws Exception {
			Map<CmfType, Map<String, String>> m = new EnumMap<CmfType, Map<String, String>>(CmfType.class);
			Map<String, CmfType> sourceTypes = new HashMap<String, CmfType>();
			for (BaseTypeT bt : mappings.getBaseType()) {
				Map<String, String> s = m.get(bt.getBaseType());
				if (s == null) {
					s = new HashMap<String, String>();
				}

				for (Map.Entry<String, String> e : bt.getMappings().entrySet()) {
					CmfType existing = sourceTypes.get(e.getKey());
					if (existing != null) {
						String msg = null;
						if (existing == bt.getBaseType()) {
							msg = String
								.format(
									"Duplicate mapping for subtype [%s] in base type [%s], will only use the first one declared",
									existing, e.getKey(), bt.getBaseType());
						} else {
							msg = String
								.format(
									"Duplicate mapping for subtype [%s] in base type [%s] from base type [%s], will only use the first one declared",
									existing, e.getKey(), bt.getBaseType());
						}
						FileBasedTypeMapperFactory.this.log.warn(msg);
						continue;
					}
					s.put(e.getKey(), e.getValue());
					sourceTypes.put(e.getKey(), bt.getBaseType());
				}

				m.put(bt.getBaseType(), Tools.freezeMap(s));
			}
			this.mappings = Tools.freezeMap(m);
		}

		@Override
		protected TypeSpec getMapping(TypeSpec sourceType) {
			Map<String, String> m = this.mappings.get(sourceType.getBaseType());
			if (m == null) { return null; }
			String subType = m.get(sourceType.getSubType());
			if (subType == null) { return null; }
			return newTypeSpec(sourceType.getBaseType(), subType);
		}
	}

	public FileBasedTypeMapperFactory() {
		super("xml");
	}

	protected InputStream getResourceStream(URI uri) throws Exception {
		String scheme = uri.getScheme();
		InputStream in = null;
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (scheme == null) {
			// First try as a path, then try in the classpath
			File f = new File(uri.getSchemeSpecificPart());
			if (f.isFile() && f.canRead()) {
				// it's a filesystem path, so use that
				@SuppressWarnings("resource")
				InputStream inputStream = new FileInputStream(f);
				in = inputStream;
			}
		}

		if ((in == null) || Tools.equals(scheme.toLowerCase(), FileBasedTypeMapperFactory.CLASSPATH)) {
			// Try it as classpath-only
			in = cl.getResourceAsStream(uri.getSchemeSpecificPart());
		}

		if (in == null) {
			// Try to decode it as a URL
			in = uri.toURL().openStream();
		}

		return in;
	}

	@Override
	public CmfTypeMapper getMapperInstance(CfgTools cfg) throws Exception {
		// Step 1: find the file required
		URI uri = new URI(cfg.getString(Setting.MAPPING_FILE));

		// Step 2: unmarshal the XML
		InputStream in = getResourceStream(uri);
		MappingsT mappings = null;
		try {
			mappings = XmlTools.unmarshal(MappingsT.class, FileBasedTypeMapperFactory.SCHEMA_NAME, in);
		} finally {
			IOUtils.closeQuietly(in);
		}

		// Step 3: build the mapper
		return new Mapper(mappings);
	}

}