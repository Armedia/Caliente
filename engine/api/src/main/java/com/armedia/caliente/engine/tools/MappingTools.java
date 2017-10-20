package com.armedia.caliente.engine.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.ConfigurationSetting;

public class MappingTools {

	private static final Logger LOG = LoggerFactory.getLogger(MappingTools.class);

	public static interface MappingValidator {
		public void validate(String key, String value) throws Exception;
	}

	private static final MappingValidator NULL_VALIDATOR = new MappingValidator() {
		@Override
		public void validate(String key, String value) throws Exception {
		}
	};

	public static boolean loadMap(Logger log, CfgTools cfg, ConfigurationSetting setting, Properties properties) {
		return MappingTools.loadMap(log, cfg, setting, properties, null);
	}

	public static boolean loadMap(Logger log, CfgTools cfg, ConfigurationSetting setting, Properties properties,
		MappingValidator validator) {

		String mapFile = cfg.getString(setting);
		if (StringUtils.isEmpty(mapFile)) { return false; }

		if (log == null) {
			log = MappingTools.LOG;
		}

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

		if (validator == null) {
			validator = MappingTools.NULL_VALIDATOR;
		}

		Properties p = new Properties();
		try {
			InputStream in = new FileInputStream(f);
			try {
				// First, try the XML format
				p.clear();
				p.loadFromXML(in);
			} catch (InvalidPropertiesFormatException e) {
				// Not XML-format, try text format
				IOUtils.closeQuietly(in);
				in = new FileInputStream(f);

				p.clear();
				p.load(in);
			} finally {
				IOUtils.closeQuietly(in);
			}

			MappingTools.copyProperties(log, p, properties, f, validator);
			return true;
		} catch (IOException e) {
			log.warn(String.format("Failed to load the properties from file [%s]", mapFile), e);
			p.clear();
			return false;
		}
	}

	private static void copyProperties(Logger log, Properties p, Properties properties, File mapFile,
		MappingValidator validator) {
		if (validator == null) {
			validator = MappingTools.NULL_VALIDATOR;
		}
		for (Object k : p.keySet()) {
			String v = p.getProperty(k.toString());
			try {
				validator.validate(k.toString(), v);
			} catch (Exception e) {
				log.error(String.format("Mapping error detected in file [%s]: [%s]->[%s]", mapFile.getAbsolutePath(),
					k.toString(), v.toString()), e);
			}
			properties.setProperty(k.toString(), v);
		}
	}
}