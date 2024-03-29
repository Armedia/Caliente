/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.BiPredicate;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.TransferException;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.ConfigurationSetting;
import com.armedia.commons.utilities.Tools;

public class MappingTools {

	private static final Logger LOG = LoggerFactory.getLogger(MappingTools.class);

	private static final BiPredicate<String, String> NULL_VALIDATOR = (a, b) -> true;

	public static boolean loadMap(Logger log, CfgTools cfg, ConfigurationSetting setting, Properties properties)
		throws TransferException {
		return MappingTools.loadMap(log, cfg, setting, properties, null);
	}

	public static boolean loadMap(Logger log, CfgTools cfg, ConfigurationSetting setting, Properties properties,
		BiPredicate<String, String> validator) throws TransferException {

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
		if (!f.isFile()) { throw new TransferException(String.format("The file [%s] is not a regular file", mapFile)); }
		if (!f.canRead()) { throw new TransferException(String.format("The file [%s] is not readable", mapFile)); }

		if (validator == null) {
			validator = MappingTools.NULL_VALIDATOR;
		}

		try (InputStream in = new FileInputStream(f)) {
			Properties p = XmlProperties.loadFromXML(in);
			MappingTools.copyProperties(log, p, properties, f, validator);
		} catch (IOException | XMLStreamException e) {
			// Not XML-format or I/O issues...
			throw new TransferException(String.format("Failed to load the properties from file [%s]", mapFile), e);
		}
		return true;
	}

	private static void copyProperties(Logger log, Properties p, Properties properties, File mapFile,
		BiPredicate<String, String> validator) throws TransferException {
		if (validator == null) {
			validator = MappingTools.NULL_VALIDATOR;
		}
		boolean ok = true;
		for (String k : p.stringPropertyNames()) {
			String v = p.getProperty(k);
			try {
				if (validator.test(k, v)) {
					properties.setProperty(k, v);
				}
			} catch (Exception e) {
				log.error("Mapping error detected in file [{}]: [{}]->[{}]", mapFile.getAbsolutePath(), k, v, e);
				ok = false;
			}
		}
		if (!ok) { throw new TransferException(String.format("Failed to load the mappings from file [%s]", mapFile)); }
	}
}