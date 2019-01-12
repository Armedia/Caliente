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
import com.armedia.commons.utilities.XmlTools;

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
					this.log.warn(String.format(
						"Failed to deserialize the value for store property [%s]:  [%s] not valid as a [%s]",
						property.getName(), property.getValue(), property.getType()));
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