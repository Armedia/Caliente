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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.xml.CmsStoreConfiguration;
import com.armedia.cmf.storage.xml.CmsStoreDefinitions;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.XmlTools;

public abstract class CmfStores {

	private static final Logger LOG = LoggerFactory.getLogger(CmfStores.class);

	private static final ReadWriteLock OBJECT_LOCK = new ReentrantReadWriteLock();
	private static final Map<String, StoreFactory<?>> OBJECT_FACTORIES;
	private static final Map<String, Object> OBJECT_STORES = new HashMap<String, Object>();

	private static final ReadWriteLock CONTENT_LOCK = new ReentrantReadWriteLock();
	private static final Map<String, StoreFactory<?>> CONTENT_FACTORIES;
	private static final Map<String, Object> CONTENT_STORES = new HashMap<String, Object>();

	static {
		@SuppressWarnings("rawtypes")
		PluggableServiceLocator<ObjectStoreFactory> objectFactories = new PluggableServiceLocator<ObjectStoreFactory>(
			ObjectStoreFactory.class);
		@SuppressWarnings("rawtypes")
		PluggableServiceLocator<ContentStoreFactory> contentFactories = new PluggableServiceLocator<ContentStoreFactory>(
			ContentStoreFactory.class);

		Map<String, StoreFactory<?>> m = new HashMap<String, StoreFactory<?>>();
		for (ObjectStoreFactory<?, ?, ?> f : objectFactories) {
			Class<? extends ObjectStore<?, ?>> c = f.getStoreClass();
			if (c == null) {
				CmfStores.LOG.warn("ObjectStoreFactory [{}] returned null for a store class", f.getClass()
					.getCanonicalName());
				continue;
			}
			String key = c.getCanonicalName();
			if (m.containsKey(key)) {
				CmfStores.LOG.warn("Duplicate factories found with target class name [{}]", key);
				continue;
			}
			m.put(key, f);
		}

		if (m.isEmpty()) {
			OBJECT_FACTORIES = Collections.emptyMap();
		} else {
			OBJECT_FACTORIES = Collections.unmodifiableMap(m);
		}

		m = new HashMap<String, StoreFactory<?>>();
		for (ContentStoreFactory<?> f : contentFactories) {
			Class<? extends ContentStore> c = f.getStoreClass();
			if (c == null) {
				CmfStores.LOG.warn("ContentStoreFactory [{}] returned null for a store class", f.getClass()
					.getCanonicalName());
				continue;
			}
			String key = c.getCanonicalName();
			if (m.containsKey(key)) {
				CmfStores.LOG.warn("Duplicate factories found with target class name [{}]", key);
				continue;
			}
			m.put(key, f);
		}

		if (m.isEmpty()) {
			CONTENT_FACTORIES = Collections.emptyMap();
		} else {
			CONTENT_FACTORIES = Collections.unmodifiableMap(m);
		}

		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		final Enumeration<URL> configs;
		try {
			configs = cl.getResources("META-INF/com/armedia/cmf/stores.xml");
			while (configs.hasMoreElements()) {
				URL config = configs.nextElement();
				Reader r = null;
				try {
					r = new InputStreamReader(config.openStream());
					CmsStoreDefinitions cfg = CmfStores.parseConfiguration(r);
					int i = 0;
					CmfStores.OBJECT_LOCK.writeLock().lock();
					try {
						for (CmsStoreConfiguration storeCfg : cfg.getObjectStores()) {
							i++;
							try {
								CmfStores.createObjectStore(storeCfg);
							} catch (Throwable t) {
								String msg = String
									.format(
										"Exception raised attempting to initialize object store #%d from the definitions at [%s]",
										i, config);
								if (CmfStores.LOG.isDebugEnabled()) {
									CmfStores.LOG.warn(msg, t);
								} else {
									CmfStores.LOG.warn(msg);
								}
							}
						}
					} finally {
						CmfStores.OBJECT_LOCK.writeLock().unlock();
					}
					CmfStores.CONTENT_LOCK.writeLock().lock();
					try {
						for (CmsStoreConfiguration storeCfg : cfg.getObjectStores()) {
							i++;
							try {
								CmfStores.createContentStore(storeCfg);
							} catch (Throwable t) {
								String msg = String
									.format(
										"Exception raised attempting to initialize content store #%d from the definitions at [%s]",
										i, config);
								if (CmfStores.LOG.isDebugEnabled()) {
									CmfStores.LOG.warn(msg, t);
								} else {
									CmfStores.LOG.warn(msg);
								}
							}
						}
					} finally {
						CmfStores.CONTENT_LOCK.writeLock().unlock();
					}
				} catch (Throwable t) {
					String msg = String.format(
						"Exception raised attempting to load the ContentStoreDefinitions from [%s]", config);
					if (CmfStores.LOG.isDebugEnabled()) {
						CmfStores.LOG.warn(msg, t);
					} else {
						CmfStores.LOG.warn(msg);
					}
				} finally {
					IOUtils.closeQuietly(r);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to load the pre-defined content store configurations"), e);
		}
	}

	protected static CmsStoreDefinitions parseConfiguration(File settings) throws StorageException, IOException,
	JAXBException {
		if (settings == null) { throw new IllegalArgumentException("Must provide a file to read the settings from"); }
		return CmfStores.parseConfiguration(settings.toURI().toURL());
	}

	protected static CmsStoreDefinitions parseConfiguration(URL settings) throws StorageException, IOException,
	JAXBException {
		Reader xml = null;
		try {
			xml = new InputStreamReader(settings.openStream());
			return CmfStores.parseConfiguration(xml);
		} finally {
			IOUtils.closeQuietly(xml);
		}
	}

	protected static CmsStoreDefinitions parseConfiguration(Reader xml) throws StorageException, JAXBException {
		return XmlTools.unmarshal(CmsStoreDefinitions.class, "stores.xsd", xml);
	}

	private static <T> T createStore(Class<T> storeClass, CmsStoreConfiguration configuration) throws StorageException,
	DuplicateStoreException {
		if (storeClass == null) { throw new IllegalArgumentException("Must provide the class of store to create"); }
		if (configuration == null) { throw new IllegalArgumentException(
			"Must provide a configuration to construct the instance from"); }
		final String id = configuration.getId();
		if (id == null) { throw new IllegalArgumentException("The configuration does not specify the store id"); }
		final String className = configuration.getClassName();
		if (className == null) { throw new IllegalArgumentException(
			"The configuration does not specify the store class"); }
		Map<String, Object> stores = null;
		Map<String, StoreFactory<?>> factories = null;
		ReadWriteLock rwLock = null;
		if (storeClass == ObjectStore.class) {
			rwLock = CmfStores.OBJECT_LOCK;
			stores = CmfStores.OBJECT_STORES;
			factories = CmfStores.OBJECT_FACTORIES;
		} else if (storeClass == ContentStore.class) {
			rwLock = CmfStores.CONTENT_LOCK;
			stores = CmfStores.CONTENT_STORES;
			factories = CmfStores.CONTENT_FACTORIES;
		}
		Lock l = rwLock.writeLock();
		l.lock();
		try {
			T dupe = storeClass.cast(stores.get(id));
			if (dupe != null) { throw new DuplicateStoreException(String.format(
				"Duplicate store requested: [%s] already exists, and is of class [%s]", id, dupe.getClass()
				.getCanonicalName())); }
			StoreFactory<?> factory = factories.get(className);
			if (factory == null) { throw new StorageException(String.format(
				"No factory found for object store class [%s]", className)); }
			T instance = storeClass.cast(factory.newInstance(configuration));
			stores.put(id, instance);
			return instance;
		} finally {
			l.unlock();
		}
	}

	public static ObjectStore<?, ?> createObjectStore(CmsStoreConfiguration configuration) throws StorageException,
	DuplicateStoreException {
		return CmfStores.createStore(ObjectStore.class, configuration);
	}

	public static ContentStore createContentStore(CmsStoreConfiguration configuration) throws StorageException,
	DuplicateStoreException {
		return CmfStores.createStore(ContentStore.class, configuration);
	}

	private static <T> T getStore(Class<T> storeClass, String name) {
		if (storeClass == null) { throw new IllegalArgumentException("Must provide the class of store to retrieve"); }
		if (name == null) { throw new IllegalArgumentException("Must provide the name of the store to retrieve"); }
		Map<String, Object> stores = null;
		ReadWriteLock rwLock = null;
		if (storeClass == ObjectStore.class) {
			rwLock = CmfStores.OBJECT_LOCK;
			stores = CmfStores.OBJECT_STORES;
		} else if (storeClass == ContentStore.class) {
			rwLock = CmfStores.CONTENT_LOCK;
			stores = CmfStores.CONTENT_STORES;
		} else {
			throw new ClassCastException(String.format("Unsupported store class: %s", storeClass.getCanonicalName()));
		}
		Lock l = rwLock.readLock();
		l.lock();
		try {
			return storeClass.cast(stores.get(name));
		} finally {
			l.unlock();
		}
	}

	public static ObjectStore<?, ?> getObjectStore(String name) {
		return CmfStores.getStore(ObjectStore.class, name);
	}

	public static ContentStore getContentStore(String name) {
		return CmfStores.getStore(ContentStore.class, name);
	}
}