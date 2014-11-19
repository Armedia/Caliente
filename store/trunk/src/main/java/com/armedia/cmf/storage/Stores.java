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
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public final class Stores {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AtomicBoolean open = new AtomicBoolean(true);
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<String, StoreFactory<?>> factories;
	private final Map<String, Store> stores = new HashMap<String, Store>();
	private final Map<String, StoreConfiguration> configurations = new HashMap<String, StoreConfiguration>();

	private final String type;

	private <F extends StoreFactory<?>> Stores(String type, Class<F> factoryClass) {
		this.type = type;
		PluggableServiceLocator<F> factories = new PluggableServiceLocator<F>(factoryClass);
		Set<String> registration = new TreeSet<String>();
		Map<String, StoreFactory<?>> m = new HashMap<String, StoreFactory<?>>();
		for (F f : factories) {
			registration.clear();
			for (String alias : f.getAliases()) {
				Object existing = m.get(alias);
				if (existing != null) {
					this.log.warn(
						"Duplicate: alias [{}] from {}StoreFactory [{}] is already registered for factory [{}]", alias,
						this.type, f.getClass().getCanonicalName(), existing.getClass().getCanonicalName());
				} else {
					m.put(alias, f);
					registration.add(alias);
				}
			}
			if (registration.isEmpty()) {
				this.log.warn(
					"{}StoreFactory [{}] didn't provide a class name or any aliases, so it will not be registered",
					type, f.getClass().getCanonicalName());
			} else {
				this.log.debug("{}StoreFactory [{}] registered with the following names: {}", type, f.getClass()
					.getCanonicalName(), registration);
			}
		}
		this.factories = Tools.freezeMap(m);
	}

	private void assertOpen() {
		if (!this.open.get()) { throw new IllegalStateException("This Stores manager has already been closed"); }
	}

	private void initStores(URL config, Collection<StoreConfiguration> storeConfigurations) {
		this.lock.writeLock().lock();
		try {
			int i = 0;
			for (StoreConfiguration storeCfg : storeConfigurations) {
				i++;
				try {
					createStore(storeCfg);
				} catch (Throwable t) {
					String msg = String.format(
						"Exception raised attempting to initialize %s store #%d from the definitions at [%s]",
						this.type, i, config);
					if (this.log.isDebugEnabled()) {
						this.log.warn(msg, t);
					} else {
						this.log.warn(msg);
					}
				}
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	private Store createStore(StoreConfiguration configuration) throws StorageException, DuplicateStoreException {
		assertOpen();
		if (configuration == null) { throw new IllegalArgumentException(
			"Must provide a configuration to construct the instance from"); }
		final String id = configuration.getId();
		if (id == null) { throw new IllegalArgumentException("The configuration does not specify the store id"); }
		final String name = configuration.getName();
		if (name == null) { throw new IllegalArgumentException("The configuration does not specify the store name"); }

		final Lock l = this.lock.writeLock();
		l.lock();
		try {
			Store dupe = this.stores.get(id);
			if (dupe != null) { throw new DuplicateStoreException(String.format(
				"Duplicate store requested: [%s] already exists, and is of class [%s]", id, dupe.getClass()
				.getCanonicalName())); }
			StoreFactory<?> factory = this.factories.get(name);
			if (factory == null) { throw new StorageException(String.format(
				"No factory found for object store class [%s]", name)); }
			Store instance = factory.newInstance(configuration);
			this.stores.put(id, instance);
			this.configurations.put(id, configuration);
			return instance;
		} finally {
			l.unlock();
		}
	}

	private Store getStore(String name) {
		assertOpen();
		if (name == null) { throw new IllegalArgumentException("Must provide the name of the store to retrieve"); }
		Lock l = this.lock.readLock();
		l.lock();
		try {
			return this.stores.get(name);
		} finally {
			l.unlock();
		}
	}

	private StoreConfiguration getConfiguration(String name) {
		assertOpen();
		if (name == null) { throw new IllegalArgumentException(
			"Must provide the name of the store configuration to retrieve"); }
		Lock l = this.lock.readLock();
		l.lock();
		try {
			StoreConfiguration cfg = this.configurations.get(name);
			return (cfg != null ? cfg.clone() : null);
		} finally {
			l.unlock();
		}
	}

	private void closeInstance() {
		if (this.open.compareAndSet(true, false)) {
			this.lock.writeLock().lock();
			try {
				for (StoreFactory<?> f : this.factories.values()) {
					try {
						f.close();
					} catch (Exception e) {
						this.log.warn(
							String.format("Exception caught closing Factory [%s]", f.getClass().getCanonicalName()), e);
					}
				}
				for (Map.Entry<String, Store> e : this.stores.entrySet()) {
					String n = e.getKey();
					Store s = e.getValue();
					try {
						s.close();
					} catch (Exception x) {
						this.log.warn(String.format("Exception caught closing ObjectStore %s[%s]", n, s.getClass()
							.getCanonicalName()), x);
					}
				}
			} finally {
				this.lock.writeLock().unlock();
			}
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(Stores.class);

	private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
	private static Stores OBJECT_STORES = null;
	private static Stores CONTENT_STORES = null;

	private static final Thread SHUTDOWN_HOOK = new Thread() {
		@Override
		public void run() {
			Stores.doClose();
		}
	};

	protected static StoreDefinitions parseConfiguration(File settings) throws StorageException, IOException,
	JAXBException {
		if (settings == null) { throw new IllegalArgumentException("Must provide a file to read the settings from"); }
		return Stores.parseConfiguration(settings.toURI().toURL());
	}

	protected static StoreDefinitions parseConfiguration(URL settings) throws StorageException, IOException,
	JAXBException {
		Reader xml = null;
		try {
			xml = new InputStreamReader(settings.openStream());
			return Stores.parseConfiguration(xml);
		} finally {
			IOUtils.closeQuietly(xml);
		}
	}

	protected static StoreDefinitions parseConfiguration(Reader xml) throws StorageException, JAXBException {
		return XmlTools.unmarshal(StoreDefinitions.class, "stores.xsd", xml);
	}

	public static void initialize() {
		Stores.LOCK.writeLock().lock();
		try {
			// If we're already initialized, dump out
			if ((Stores.OBJECT_STORES != null) || (Stores.CONTENT_STORES != null)) { return; }

			Stores.OBJECT_STORES = new Stores("Object", ObjectStoreFactory.class);
			Stores.CONTENT_STORES = new Stores("Content", ContentStoreFactory.class);

			Runtime.getRuntime().addShutdownHook(Stores.SHUTDOWN_HOOK);

			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			final Enumeration<URL> configs;
			try {
				configs = cl.getResources("META-INF/com/armedia/cmf/stores.xml");
			} catch (IOException e) {
				Stores.LOG.error("Failed to load the pre-defined content store configurations", e);
				return;
			}

			// We have the set of configurations, we iterate through each one
			while (configs.hasMoreElements()) {
				URL config = configs.nextElement();
				Reader r = null;
				final StoreDefinitions cfg;
				try {
					r = new InputStreamReader(config.openStream());
					cfg = Stores.parseConfiguration(r);
				} catch (Exception e) {
					String msg = String.format("Exception raised attempting to load the StoreDefinitions from [%s]",
						config);
					if (Stores.LOG.isDebugEnabled()) {
						Stores.LOG.warn(msg, e);
					} else {
						Stores.LOG.warn(msg);
					}
					continue;
				} finally {
					IOUtils.closeQuietly(r);
				}

				Stores.OBJECT_STORES.initStores(config, cfg.getObjectStores());
				Stores.CONTENT_STORES.initStores(config, cfg.getContentStores());
			}
		} finally {
			Stores.LOCK.writeLock().unlock();
		}
	}

	private static boolean doClose() {
		Stores.LOCK.writeLock().lock();
		boolean ret = false;
		try {
			if (Stores.OBJECT_STORES != null) {
				Stores.OBJECT_STORES.closeInstance();
				Stores.OBJECT_STORES = null;
				ret = true;
			}
			if (Stores.CONTENT_STORES != null) {
				Stores.CONTENT_STORES.closeInstance();
				Stores.CONTENT_STORES = null;
				ret = true;
			}
			return ret;
		} finally {
			Stores.LOCK.writeLock().unlock();
		}
	}

	public static void close() {
		if (Stores.doClose()) {
			Runtime.getRuntime().removeShutdownHook(Stores.SHUTDOWN_HOOK);
		}
	}

	private static Stores assertValid(Stores instance) {
		if (instance == null) { throw new IllegalStateException("The requested Stores instance is no longer valid"); }
		instance.assertOpen();
		return instance;
	}

	public static ObjectStore<?, ?> createObjectStore(StoreConfiguration configuration) throws StorageException,
	DuplicateStoreException {
		Stores.initialize();
		Stores.LOCK.readLock().lock();
		try {
			return ObjectStore.class.cast(Stores.assertValid(Stores.OBJECT_STORES).createStore(configuration));
		} finally {
			Stores.LOCK.readLock().unlock();
		}
	}

	public static ContentStore createContentStore(StoreConfiguration configuration) throws StorageException,
	DuplicateStoreException {
		Stores.initialize();
		Stores.LOCK.readLock().lock();
		try {
			return ContentStore.class.cast(Stores.assertValid(Stores.CONTENT_STORES).createStore(configuration));
		} finally {
			Stores.LOCK.readLock().unlock();
		}
	}

	public static StoreConfiguration getObjectStoreConfiguration(String name) {
		Stores.initialize();
		Stores.LOCK.readLock().lock();
		try {
			return Stores.assertValid(Stores.OBJECT_STORES).getConfiguration(name);
		} finally {
			Stores.LOCK.readLock().unlock();
		}
	}

	public static ObjectStore<?, ?> getObjectStore(String name) {
		Stores.initialize();
		Stores.LOCK.readLock().lock();
		try {
			return ObjectStore.class.cast(Stores.assertValid(Stores.OBJECT_STORES).getStore(name));
		} finally {
			Stores.LOCK.readLock().unlock();
		}
	}

	public static StoreConfiguration getContentStoreConfiguration(String name) {
		Stores.initialize();
		Stores.LOCK.readLock().lock();
		try {
			return Stores.assertValid(Stores.CONTENT_STORES).getConfiguration(name);
		} finally {
			Stores.LOCK.readLock().unlock();
		}
	}

	public static ContentStore getContentStore(String name) {
		Stores.initialize();
		Stores.LOCK.readLock().lock();
		try {
			return ContentStore.class.cast(Stores.assertValid(Stores.CONTENT_STORES).getStore(name));
		} finally {
			Stores.LOCK.readLock().unlock();
		}
	}
}