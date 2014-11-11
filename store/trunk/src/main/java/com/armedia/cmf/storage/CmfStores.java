package com.armedia.cmf.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.xml.StoreConfiguration;
import com.armedia.cmf.storage.xml.StoreDefinitions;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.XmlTools;

public abstract class CmfStores {

	private static final Logger LOG = LoggerFactory.getLogger(CmfStores.class);

	private static final AtomicBoolean OPEN = new AtomicBoolean(false);

	private static final ReadWriteLock OBJECT_LOCK = new ReentrantReadWriteLock();
	private static final Map<String, StoreFactory<?>> OBJECT_FACTORIES = new HashMap<String, StoreFactory<?>>();
	private static final Map<String, Store> OBJECT_STORES = new HashMap<String, Store>();

	private static final ReadWriteLock CONTENT_LOCK = new ReentrantReadWriteLock();
	private static final Map<String, StoreFactory<?>> CONTENT_FACTORIES = new HashMap<String, StoreFactory<?>>();
	private static final Map<String, Store> CONTENT_STORES = new HashMap<String, Store>();

	private static final Thread SHUTDOWN_HOOK = new Thread() {
		@Override
		public void run() {
			CmfStores.close();
		}
	};

	static {
		CmfStores.initFactories(ObjectStoreFactory.class, CmfStores.OBJECT_FACTORIES, "Object");
		CmfStores.initFactories(ContentStoreFactory.class, CmfStores.CONTENT_FACTORIES, "Content");
		CmfStores.OPEN.set(true);
		Runtime.getRuntime().addShutdownHook(CmfStores.SHUTDOWN_HOOK);
		CmfStores.initStores();
	}

	private static void assertOpen() {
		if (!CmfStores.OPEN.get()) { throw new IllegalStateException("The CmfStores framework has already been closed"); }
	}

	private static <F extends StoreFactory<?>> void initFactories(Class<F> klass, Map<String, StoreFactory<?>> m,
		String type) {
		PluggableServiceLocator<F> factories = new PluggableServiceLocator<F>(klass);
		Set<String> registration = new TreeSet<String>();
		for (StoreFactory<?> f : factories) {
			for (String alias : f.getAliases()) {
				Object existing = m.get(alias);
				if (existing != null) {
					CmfStores.LOG.warn(
						"Duplicate: alias [{}] from {}StoreFactory [{}] is already registered for factory [{}]", alias,
						type, f.getClass().getCanonicalName(), existing.getClass().getCanonicalName());
				} else {
					m.put(alias, f);
					registration.add(alias);
				}
			}
			if (registration.isEmpty()) {
				CmfStores.LOG.warn(
					"{}StoreFactory [{}] didn't provide a class name or any aliases, so it will not be registered",
					type, f.getClass().getCanonicalName());
			} else {
				CmfStores.LOG.debug("{}StoreFactory [{}] registered with the following names: {}", type, f.getClass()
					.getCanonicalName(), registration);
			}
		}
	}

	private static <S extends Store> void initStores(Class<S> storeClass, Map<String, Store> m, String type,
		URL config, Collection<StoreConfiguration> storeConfigurations) {
		int i = 0;
		for (StoreConfiguration storeCfg : storeConfigurations) {
			i++;
			try {
				CmfStores.createStore(storeClass, storeCfg);
			} catch (Throwable t) {
				String msg = String.format(
					"Exception raised attempting to initialize %s store #%d from the definitions at [%s]", type, i,
					config);
				if (CmfStores.LOG.isDebugEnabled()) {
					CmfStores.LOG.warn(msg, t);
				} else {
					CmfStores.LOG.warn(msg);
				}
			}
		}
	}

	private static void initStores() {
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		final Enumeration<URL> configs;
		try {
			configs = cl.getResources("META-INF/com/armedia/cmf/stores.xml");
		} catch (IOException e) {
			CmfStores.LOG.error("Failed to load the pre-defined content store configurations", e);
			return;
		}

		while (configs.hasMoreElements()) {
			URL config = configs.nextElement();
			Reader r = null;
			final StoreDefinitions cfg;
			try {
				r = new InputStreamReader(config.openStream());
				cfg = CmfStores.parseConfiguration(r);
			} catch (Exception e) {
				String msg = String
					.format("Exception raised attempting to load the StoreDefinitions from [%s]", config);
				if (CmfStores.LOG.isDebugEnabled()) {
					CmfStores.LOG.warn(msg, e);
				} else {
					CmfStores.LOG.warn(msg);
				}
				continue;
			} finally {
				IOUtils.closeQuietly(r);
			}

			CmfStores.OBJECT_LOCK.writeLock().lock();
			try {
				CmfStores.initStores(ObjectStore.class, CmfStores.OBJECT_STORES, "object", config,
					cfg.getObjectStores());
			} finally {
				CmfStores.OBJECT_LOCK.writeLock().unlock();
			}

			CmfStores.CONTENT_LOCK.writeLock().lock();
			try {
				CmfStores.initStores(ContentStore.class, CmfStores.CONTENT_STORES, "content", config,
					cfg.getContentStores());
			} finally {
				CmfStores.CONTENT_LOCK.writeLock().unlock();
			}
		}
	}

	protected static StoreDefinitions parseConfiguration(File settings) throws StorageException, IOException,
		JAXBException {
		if (settings == null) { throw new IllegalArgumentException("Must provide a file to read the settings from"); }
		return CmfStores.parseConfiguration(settings.toURI().toURL());
	}

	protected static StoreDefinitions parseConfiguration(URL settings) throws StorageException, IOException,
		JAXBException {
		Reader xml = null;
		try {
			xml = new InputStreamReader(settings.openStream());
			return CmfStores.parseConfiguration(xml);
		} finally {
			IOUtils.closeQuietly(xml);
		}
	}

	protected static StoreDefinitions parseConfiguration(Reader xml) throws StorageException, JAXBException {
		return XmlTools.unmarshal(StoreDefinitions.class, "stores.xsd", xml);
	}

	private static <T extends Store> T createStore(Class<T> storeClass, StoreConfiguration configuration)
		throws StorageException, DuplicateStoreException {
		CmfStores.assertOpen();
		if (storeClass == null) { throw new IllegalArgumentException("Must provide the class of store to create"); }
		if (configuration == null) { throw new IllegalArgumentException(
			"Must provide a configuration to construct the instance from"); }
		final String id = configuration.getId();
		if (id == null) { throw new IllegalArgumentException("The configuration does not specify the store id"); }
		final String name = configuration.getName();
		if (name == null) { throw new IllegalArgumentException("The configuration does not specify the store class"); }
		Map<String, Store> stores = null;
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
			StoreFactory<?> factory = factories.get(name);
			if (factory == null) { throw new StorageException(String.format(
				"No factory found for object store class [%s]", name)); }
			T instance = storeClass.cast(factory.newInstance(configuration));
			stores.put(id, instance);
			return instance;
		} finally {
			l.unlock();
		}
	}

	public static ObjectStore<?, ?> createObjectStore(StoreConfiguration configuration) throws StorageException,
		DuplicateStoreException {
		return CmfStores.createStore(ObjectStore.class, configuration);
	}

	public static ContentStore createContentStore(StoreConfiguration configuration) throws StorageException,
		DuplicateStoreException {
		return CmfStores.createStore(ContentStore.class, configuration);
	}

	private static <T extends Store> T getStore(Class<T> storeClass, String name) {
		CmfStores.assertOpen();
		if (storeClass == null) { throw new IllegalArgumentException("Must provide the class of store to retrieve"); }
		if (name == null) { throw new IllegalArgumentException("Must provide the name of the store to retrieve"); }
		Map<String, Store> stores = null;
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

	/*
	private static final Map<String, StoreFactory<?>> OBJECT_FACTORIES;
	private static final Map<String, Object> OBJECT_STORES = new HashMap<String, Object>();

	private static final ReadWriteLock CONTENT_LOCK = new ReentrantReadWriteLock();
	private static final Map<String, StoreFactory<?>> CONTENT_FACTORIES;
	private static final Map<String, Object> CONTENT_STORES = new HashMap<String, Object>();
	 */
	private static void close() {
		if (CmfStores.OPEN.compareAndSet(true, false)) {
			CmfStores.OBJECT_LOCK.writeLock().lock();
			CmfStores.CONTENT_LOCK.writeLock().lock();
			try {
				for (StoreFactory<?> f : CmfStores.OBJECT_FACTORIES.values()) {
					try {
						f.close();
					} catch (Exception e) {
						CmfStores.LOG.warn(String.format("Exception caught closing ObjectStoreFactory [%s]", f
							.getClass().getCanonicalName()), e);
					}
				}
				for (Map.Entry<String, Store> e : CmfStores.OBJECT_STORES.entrySet()) {
					String n = e.getKey();
					Store s = e.getValue();
					try {
						s.close();
					} catch (Exception x) {
						CmfStores.LOG.warn(String.format("Exception caught closing ObjectStore %s[%s]", n, s.getClass()
							.getCanonicalName()), x);
					}
				}
				for (StoreFactory<?> f : CmfStores.CONTENT_FACTORIES.values()) {
					try {
						f.close();
					} catch (Exception e) {
						CmfStores.LOG.warn(String.format("Exception caught closing ContentStoreFactory [%s]", f
							.getClass().getCanonicalName()), e);
					}
				}
				for (Map.Entry<String, Store> e : CmfStores.CONTENT_STORES.entrySet()) {
					String n = e.getKey();
					Store s = e.getValue();
					try {
						s.close();
					} catch (Exception x) {
						CmfStores.LOG.warn(String.format("Exception caught closing ObjectStore %s[%s]", n, s.getClass()
							.getCanonicalName()), x);
					}
				}
			} finally {
				CmfStores.CONTENT_LOCK.writeLock().unlock();
				CmfStores.OBJECT_LOCK.writeLock().unlock();
			}
		}
	}
}