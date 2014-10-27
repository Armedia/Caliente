package com.armedia.cmf.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.xml.CmsObjectStoreConfiguration;
import com.armedia.cmf.storage.xml.CmsObjectStoreDefinitions;
import com.armedia.commons.utilities.XmlTools;

public abstract class ObjectStoreFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ObjectStoreFactory.class);

	private final Class<? extends ObjectStore> storeClass;

	protected ObjectStoreFactory(Class<? extends ObjectStore> storeClass) {
		this.storeClass = storeClass;
	}

	protected final Class<? extends ObjectStore> getObjectStoreClass() {
		return this.storeClass;
	}

	private static final Map<String, ObjectStoreFactory> FACTORIES;

	private static final ReadWriteLock STORE_LOCK = new ReentrantReadWriteLock();
	private static final Map<String, ObjectStore> STORES = new HashMap<String, ObjectStore>();

	static {
		ServiceLoader<ObjectStoreFactory> loader = ServiceLoader.load(ObjectStoreFactory.class);
		Map<String, ObjectStoreFactory> m = new HashMap<String, ObjectStoreFactory>();
		for (ObjectStoreFactory f : loader) {
			Class<? extends ObjectStore> c = f.getObjectStoreClass();
			if (c == null) {
				ObjectStoreFactory.LOG.warn("CmsObjectStoreFactory [{}] returned null for an object store class", f
					.getClass().getCanonicalName());
				continue;
			}
			String key = c.getCanonicalName();
			if (m.containsKey(key)) {
				ObjectStoreFactory.LOG.warn("Duplicate factories found with target class name [{}]", key);
				continue;
			}
			m.put(key, f);
		}
		if (m.isEmpty()) {
			FACTORIES = Collections.emptyMap();
		} else {
			FACTORIES = Collections.unmodifiableMap(m);
		}

		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		final Enumeration<URL> configs;
		try {
			configs = cl.getResources("META-INF/com/armedia/cmf/objectstore.xml");
			while (configs.hasMoreElements()) {
				URL config = configs.nextElement();
				Reader r = null;
				try {
					r = new InputStreamReader(config.openStream());
					CmsObjectStoreDefinitions cfg = ObjectStoreFactory.parseConfiguration(r);
					int i = 0;
					for (CmsObjectStoreConfiguration storeCfg : cfg.getObjectStores()) {
						i++;
						try {
							ObjectStoreFactory.createInstance(storeCfg);
						} catch (Throwable t) {
							String msg = String
								.format(
									"Exception raised attempting to initialize object store #%d from the definitions at [%s]",
									i, config);
							if (ObjectStoreFactory.LOG.isDebugEnabled()) {
								ObjectStoreFactory.LOG.warn(msg, t);
							} else {
								ObjectStoreFactory.LOG.warn(msg);
							}
						}
					}
				} catch (Throwable t) {
					String msg = String.format(
						"Exception raised attempting to load the CmsObjectStoreDefinitions from [%s]", config);
					if (ObjectStoreFactory.LOG.isDebugEnabled()) {
						ObjectStoreFactory.LOG.warn(msg, t);
					} else {
						ObjectStoreFactory.LOG.warn(msg);
					}
				} finally {
					IOUtils.closeQuietly(r);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to load the pre-defined object store configurations"), e);
		}
	}

	protected static CmsObjectStoreDefinitions parseConfiguration(File settings) throws StorageException,
	IOException, JAXBException {
		if (settings == null) { throw new IllegalArgumentException("Must provide a file to read the settings from"); }
		return ObjectStoreFactory.parseConfiguration(settings.toURI().toURL());
	}

	protected static CmsObjectStoreDefinitions parseConfiguration(URL settings) throws StorageException,
	IOException, JAXBException {
		Reader xml = null;
		try {
			xml = new InputStreamReader(settings.openStream());
			return ObjectStoreFactory.parseConfiguration(xml);
		} finally {
			IOUtils.closeQuietly(xml);
		}
	}

	protected static CmsObjectStoreDefinitions parseConfiguration(Reader xml) throws StorageException, JAXBException {
		return XmlTools.unmarshal(CmsObjectStoreDefinitions.class, "objectstore.xsd", xml);
	}

	public static ObjectStore createInstance(CmsObjectStoreConfiguration configuration) throws StorageException,
	DuplicateObjectStoreException {
		if (configuration == null) { throw new IllegalArgumentException(
			"Must provide a configuration to construct the instance from"); }
		final String id = configuration.getId();
		if (id == null) { throw new IllegalArgumentException("The configuration does not specify the object store id"); }
		final String className = configuration.getClassName();
		if (className == null) { throw new IllegalArgumentException(
			"The configuration does not specify the object store class"); }
		Lock l = ObjectStoreFactory.STORE_LOCK.writeLock();
		l.lock();
		try {
			ObjectStore dupe = ObjectStoreFactory.STORES.get(id);
			if (dupe != null) { throw new DuplicateObjectStoreException(String.format(
				"Duplicate store requested: [%s] already exists, and is of class [%s]", id, dupe.getClass()
				.getCanonicalName())); }
			ObjectStoreFactory factory = ObjectStoreFactory.FACTORIES.get(className);
			if (factory == null) { throw new StorageException(String.format(
				"No factory found for object store class [%s]", className)); }
			ObjectStore instance = factory.newInstance(configuration);
			ObjectStoreFactory.STORES.put(id, instance);
			return instance;
		} finally {
			l.unlock();
		}
	}

	public static ObjectStore getInstance(String name) {
		Lock l = ObjectStoreFactory.STORE_LOCK.readLock();
		l.lock();
		try {
			return ObjectStoreFactory.STORES.get(name);
		} finally {
			l.unlock();
		}
	}

	protected abstract ObjectStore newInstance(CmsObjectStoreConfiguration cfg) throws StorageException;
}