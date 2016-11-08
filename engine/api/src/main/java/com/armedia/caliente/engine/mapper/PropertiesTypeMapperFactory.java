package com.armedia.caliente.engine.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfTypeMapperFactory;
import com.armedia.commons.utilities.BinaryMemoryBuffer;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.ConfigurationSetting;
import com.armedia.commons.utilities.Tools;

public class PropertiesTypeMapperFactory extends CmfTypeMapperFactory {

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

	private class Mapper extends CmfTypeMapper {

		private final Map<String, String> mappings;

		private Mapper(Properties properties) throws Exception {
			Map<String, String> m = new HashMap<String, String>();
			for (Object o : properties.keySet()) {
				if (o == null) {
					continue;
				}
				final String src = o.toString();
				final String tgt = properties.getProperty(src);
				m.put(src, tgt);
			}
			this.mappings = Tools.freezeMap(m);
		}

		@Override
		protected String getMapping(String sourceType) {
			return this.mappings.get(sourceType);
		}
	}

	public PropertiesTypeMapperFactory() {
		super("properties");
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

		if ((in == null) || Tools.equals(scheme.toLowerCase(), PropertiesTypeMapperFactory.CLASSPATH)) {
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
		if (in == null) { throw new Exception(String.format("Failed to locate the resource from [%s]", uri)); }

		// Read the raw file into memory
		BinaryMemoryBuffer buf = new BinaryMemoryBuffer();
		try {
			IOUtils.copy(in, buf);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(buf);
		}

		Properties properties = new Properties();
		try {
			properties.loadFromXML(buf.getInputStream());
		} catch (InvalidPropertiesFormatException e) {
			// Not in XML format, try "classic" format...
			properties.clear();
			properties.load(buf.getInputStream());
		}

		// Step 3: build the mapper
		return new Mapper(properties);
	}

}