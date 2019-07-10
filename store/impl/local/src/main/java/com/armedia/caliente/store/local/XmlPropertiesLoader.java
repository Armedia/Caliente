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
package com.armedia.caliente.store.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.commons.utilities.xml.XmlTools;

public abstract class XmlPropertiesLoader<P extends XmlProperty, S extends XmlStoreProperties<P>> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String schema;
	private final Class<S> rootClass;

	protected XmlPropertiesLoader(String schema, Class<S> rootClass) {
		this.schema = schema;
		this.rootClass = rootClass;
	}

	public final boolean loadProperties(File propertiesFile, Map<String, CmfValue> properties)
		throws CmfStorageException {
		if (!propertiesFile.exists()) { return false; }
		// Allow an empty file...
		if (propertiesFile.length() == 0) { return true; }
		try (InputStream in = new FileInputStream(propertiesFile)) {
			S p = XmlTools.unmarshal(this.rootClass, this.schema, in);
			properties.clear();
			for (XmlProperty property : p.getProperty()) {
				CmfValueSerializer deserializer = CmfValueSerializer.get(property.getType());
				if (deserializer == null) {
					continue;
				}
				final CmfValue v;
				try {
					v = deserializer.deserialize(property.getValue());
				} catch (Exception e) {
					this.log.warn("Failed to deserialize the value for store property [{}]:  [{}] not valid as a [{}]",
						property.getName(), property.getValue(), property.getType());
					continue;
				}
				if ((v != null) && !v.isNull()) {
					properties.put(property.getName(), v);
				}
			}
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			throw new CmfStorageException("IOException attempting to load the properties file", e);
		} catch (JAXBException e) {
			throw new CmfStorageException("Failed to load the stored properties", e);
		}
	}
}