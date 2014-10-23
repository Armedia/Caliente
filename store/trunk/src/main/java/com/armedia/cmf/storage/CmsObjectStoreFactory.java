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

public abstract class CmsObjectStoreFactory {

	private static final Logger LOG = LoggerFactory.getLogger(CmsObjectStoreFactory.class);

	private final Class<? extends CmsObjectStore> storeClass;

	protected CmsObjectStoreFactory(Class<? extends CmsObjectStore> storeClass) {
		this.storeClass = storeClass;
	}

	protected final Class<? extends CmsObjectStore> getObjectStoreClass() {
		return this.storeClass;
	}

	private static final Map<String, CmsObjectStoreFactory> FACTORIES;

	private static final ReadWriteLock STORE_LOCK = new ReentrantReadWriteLock();
	private static final Map<String, CmsObjectStore> STORES = new HashMap<String, CmsObjectStore>();

	static {
		ServiceLoader<CmsObjectStoreFactory> loader = ServiceLoader.load(CmsObjectStoreFactory.class);
		Map<String, CmsObjectStoreFactory> m = new HashMap<String, CmsObjectStoreFactory>();
		for (CmsObjectStoreFactory f : loader) {
			Class<? extends CmsObjectStore> c = f.getObjectStoreClass();
			if (c == null) {
				CmsObjectStoreFactory.LOG.warn("CmsObjectStoreFactory [%s] returned null for an object store class", f
					.getClass().getCanonicalName());
				continue;
			}
			String key = c.getCanonicalName();
			if (m.containsKey(key)) {
				CmsObjectStoreFactory.LOG.warn("Duplicate factories found with target class name [%s]", key);
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
					CmsObjectStoreDefinitions cfg = CmsObjectStoreFactory.parseConfiguration(r);
					int i = 0;
					for (CmsObjectStoreConfiguration storeCfg : cfg.getObjectStores()) {
						i++;
						try {
							CmsObjectStoreFactory.createInstance(storeCfg);
						} catch (Throwable t) {
							String msg = String
								.format(
									"Exception raised attempting to initialize object store #%d from the definitions at [%s]",
									i, config);
							if (CmsObjectStoreFactory.LOG.isDebugEnabled()) {
								CmsObjectStoreFactory.LOG.warn(msg, t);
							} else {
								CmsObjectStoreFactory.LOG.warn(msg);
							}
						}
					}
				} catch (Throwable t) {
					String msg = String.format(
						"Exception raised attempting to load the CmsObjectStoreDefinitions from [%s]", config);
					if (CmsObjectStoreFactory.LOG.isDebugEnabled()) {
						CmsObjectStoreFactory.LOG.warn(msg, t);
					} else {
						CmsObjectStoreFactory.LOG.warn(msg);
					}
				} finally {
					IOUtils.closeQuietly(r);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to load the pre-defined object store configurations"), e);
		}
	}

	protected static CmsObjectStoreDefinitions parseConfiguration(File settings) throws CmsStorageException,
	IOException, JAXBException {
		if (settings == null) { throw new IllegalArgumentException("Must provide a file to read the settings from"); }
		return CmsObjectStoreFactory.parseConfiguration(settings.toURI().toURL());
	}

	protected static CmsObjectStoreDefinitions parseConfiguration(URL settings) throws CmsStorageException,
	IOException, JAXBException {
		Reader xml = null;
		try {
			xml = new InputStreamReader(settings.openStream());
			return CmsObjectStoreFactory.parseConfiguration(xml);
		} finally {
			IOUtils.closeQuietly(xml);
		}
	}

	protected static CmsObjectStoreDefinitions parseConfiguration(Reader xml) throws CmsStorageException, JAXBException {
		return XmlTools.unmarshal(CmsObjectStoreDefinitions.class, "objectstore.xsd", xml);
	}

	public static CmsObjectStore createInstance(CmsObjectStoreConfiguration configuration) throws CmsStorageException,
	CmsDuplicateObjectStoreException {
		if (configuration == null) { throw new IllegalArgumentException(
			"Must provide a configuration to construct the instance from"); }
		final String id = configuration.getId();
		if (id == null) { throw new IllegalArgumentException("The configuration does not specify the object store id"); }
		final String className = configuration.getClassName();
		if (className == null) { throw new IllegalArgumentException(
			"The configuration does not specify the object store class"); }
		Lock l = CmsObjectStoreFactory.STORE_LOCK.writeLock();
		l.lock();
		try {
			CmsObjectStore dupe = CmsObjectStoreFactory.STORES.get(id);
			if (dupe != null) { throw new CmsDuplicateObjectStoreException(String.format(
				"Duplicate store requested: [%s] already exists, and is of class [%s]", id, dupe.getClass()
				.getCanonicalName())); }
			CmsObjectStoreFactory factory = CmsObjectStoreFactory.FACTORIES.get(className);
			if (factory == null) { throw new CmsStorageException(String.format(
				"No factory found for object store class [%s]", className)); }
			CmsObjectStore instance = factory.newInstance(configuration);
			CmsObjectStoreFactory.STORES.put(id, instance);
			return instance;
		} finally {
			l.unlock();
		}
	}

	public static CmsObjectStore getInstance(String name) {
		Lock l = CmsObjectStoreFactory.STORE_LOCK.readLock();
		l.lock();
		try {
			return CmsObjectStoreFactory.STORES.get(name);
		} finally {
			l.unlock();
		}
	}

	protected abstract CmsObjectStore newInstance(CmsObjectStoreConfiguration cfg) throws CmsStorageException;
}