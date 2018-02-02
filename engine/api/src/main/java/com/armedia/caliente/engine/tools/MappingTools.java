package com.armedia.caliente.engine.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.TransferEngineException;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.ConfigurationSetting;
import com.armedia.commons.utilities.Tools;

public class MappingTools {

	private static final Logger LOG = LoggerFactory.getLogger(MappingTools.class);

	public static interface MappingValidator {
		public boolean validate(String key, String value) throws Exception;
	}

	private static final MappingValidator NULL_VALIDATOR = new MappingValidator() {
		@Override
		public boolean validate(String key, String value) {
			return true;
		}
	};

	public static boolean loadMap(Logger log, CfgTools cfg, ConfigurationSetting setting, Properties properties)
		throws TransferEngineException {
		return MappingTools.loadMap(log, cfg, setting, properties, null);
	}

	public static boolean loadMap(Logger log, CfgTools cfg, ConfigurationSetting setting, Properties properties,
		MappingValidator validator) throws TransferEngineException {

		String mapFile = cfg.getString(setting);
		if (StringUtils.isEmpty(mapFile)) { return false; }

		if (log == null) {
			log = MappingTools.LOG;
		}

		File f = Tools.canonicalize(new File(mapFile));
		if (!f.exists()) {
			/*
			// TODO: Detect if it was the default value we got...
			if (!cfg.isDefault(setting, mapFile)) {
				throw new TransferEngineException(String.format("The file [%s] does not exist", mapFile));
			}
			 */
			log.warn("The file [{}] does not exist", mapFile);
			return false;
		}
		if (!f.isFile()) { throw new TransferEngineException(
			String.format("The file [%s] is not a regular file", mapFile)); }
		if (!f
			.canRead()) { throw new TransferEngineException(String.format("The file [%s] is not readable", mapFile)); }

		if (validator == null) {
			validator = MappingTools.NULL_VALIDATOR;
		}

		try (InputStream in = new FileInputStream(f)) {
			Properties p = XmlProperties.loadFromXML(in);
			MappingTools.copyProperties(log, p, properties, f, validator);
		} catch (IOException | XMLStreamException e) {
			// Not XML-format or I/O issues...
			throw new TransferEngineException(String.format("Failed to load the properties from file [%s]", mapFile),
				e);
		}
		return true;
	}

	private static void copyProperties(Logger log, Properties p, Properties properties, File mapFile,
		MappingValidator validator) throws TransferEngineException {
		if (validator == null) {
			validator = MappingTools.NULL_VALIDATOR;
		}
		boolean ok = true;
		for (String k : p.stringPropertyNames()) {
			String v = p.getProperty(k);
			try {
				if (validator.validate(k, v)) {
					properties.setProperty(k, v);
				}
			} catch (Exception e) {
				log.error(String.format("Mapping error detected in file [%s]: [%s]->[%s]", mapFile.getAbsolutePath(), k,
					v.toString()), e);
				ok = false;
			}
		}
		if (!ok) { throw new TransferEngineException(
			String.format("Failed to load the mappings from file [%s]", mapFile)); }
	}
}