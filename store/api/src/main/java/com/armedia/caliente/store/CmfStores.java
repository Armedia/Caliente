package com.armedia.caliente.store;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.store.xml.StoreDefinitions;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.XmlTools;

public final class CmfStores {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AtomicBoolean open = new AtomicBoolean(true);
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<String, CmfStoreFactory<?>> factories;
	private final Map<String, CmfStore<?>> cmfStores = new HashMap<>();
	private final Map<String, CmfStorePrep> cmfPreps = new HashMap<>();
	private final Map<String, StoreConfiguration> configurations = new HashMap<>();

	private final String type;

	private <F extends CmfStoreFactory<?>> CmfStores(String type, Class<F> factoryClass) {
		this.type = type;
		PluggableServiceLocator<F> factories = new PluggableServiceLocator<>(factoryClass);
		factories.setHideErrors(true);
		Set<String> registration = new TreeSet<>();
		Map<String, CmfStoreFactory<?>> m = new HashMap<>();
		for (F f : factories) {
			registration.clear();
			for (String alias : f.getAliases()) {
				Object existing = m.get(alias);
				if (existing != null) {
					this.log.warn(
						"Duplicate: alias [{}] from {}CmfStoreFactory [{}] is already registered for factory [{}]",
						alias, this.type, f.getClass().getCanonicalName(), existing.getClass().getCanonicalName());
				} else {
					m.put(alias, f);
					registration.add(alias);
				}
			}
			if (registration.isEmpty()) {
				this.log.warn(
					"{}CmfStoreFactory [{}] didn't provide a class name or any aliases, so it will not be registered",
					type, f.getClass().getCanonicalName());
			} else {
				this.log.debug("{}CmfStoreFactory [{}] registered with the following names: {}", type,
					f.getClass().getCanonicalName(), registration);
			}
		}
		this.factories = m;
	}

	private void assertOpen() {
		if (!this.open.get()) { throw new IllegalStateException("This CmfStores manager has already been closed"); }
	}

	private void initConfigurations(URL config, Collection<StoreConfiguration> storeConfigurations) {
		this.lock.writeLock().lock();
		try {
			for (StoreConfiguration storeCfg : storeConfigurations) {
				this.configurations.put(storeCfg.getId(), storeCfg);
			}
		} finally {
			this.lock.writeLock().unlock();
		}
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

	private final CmfStorePrep prepareStore(StoreConfiguration configuration, boolean cleanData)
		throws CmfStorageException {
		final String prepClassName = configuration.getPrep();

		// If no preparation class is specified, do nothing
		if (prepClassName == null) { return null; }

		// Does the class exist?
		final Class<?> prepClass;
		try {
			prepClass = Class.forName(prepClassName);
		} catch (ClassNotFoundException e) {
			throw new CmfStorageException(String.format("Failed to find store preparation class [%s] for store [%s]",
				prepClassName, configuration.getId()), e);
		}

		// Is this the right type of class?
		if (!CmfStorePrep.class.isAssignableFrom(prepClass)) {
			throw new CmfStorageException(
				String.format("The store preparation class [%s] for store [%s] is not a valid sublcass of %s",
					prepClassName, configuration.getId(), CmfStorePrep.class.getCanonicalName()));
		}

		// Instantiate the preparation object...
		final CmfStorePrep prepInstance;
		try {
			prepInstance = CmfStorePrep.class.cast(prepClass.newInstance());
		} catch (Exception e) {
			throw new CmfStorageException(
				String.format("Failed to instantiate the store preparation class [%s] for store [%s]", prepClassName,
					configuration.getId()),
				e);
		}

		// Execute the actual preparation
		try {
			prepInstance.prepareStore(configuration, cleanData);
		} catch (CmfStoragePreparationException e) {
			throw new CmfStorageException(
				String.format("Failed to execute the store preparation for store [%s] with class [%s]",
					configuration.getId(), prepClassName),
				e);
		}
		return prepInstance;
	}

	private CmfStore<?> createStore(StoreConfiguration configuration)
		throws CmfStorageException, DuplicateCmfStoreException {
		assertOpen();
		if (configuration == null) {
			throw new IllegalArgumentException("Must provide a configuration to construct the instance from");
		}
		final String id = configuration.getId();
		if (id == null) { throw new IllegalArgumentException("The configuration does not specify the store id"); }
		final String type = configuration.getType();
		if (type == null) { throw new IllegalArgumentException("The configuration does not specify the store type"); }

		final Lock l = this.lock.writeLock();
		l.lock();
		try {
			CmfStore<?> dupe = this.cmfStores.get(id);
			if (dupe != null) {
				throw new DuplicateCmfStoreException(
					String.format("Duplicate store requested: [%s] already exists, and is of class [%s]", id,
						dupe.getClass().getCanonicalName()));
			}
			CmfStoreFactory<?> factory = this.factories.get(type);
			if (factory == null) {
				throw new CmfStorageException(String.format("No factory found for object store type [%s]", type));
			}
			CfgTools cfg = new CfgTools(configuration.getEffectiveSettings());

			final boolean cleanData = cfg.getBoolean(CmfStoreFactory.CFG_CLEAN_DATA, false);
			final CmfStorePrep prep = prepareStore(configuration, cleanData);
			CmfStore<?> instance = factory.newInstance(configuration, cleanData, prep);
			this.cmfStores.put(id, instance);
			if (prep != null) {
				this.cmfPreps.put(id, prep);
			}
			this.configurations.put(id, configuration);
			return instance;
		} finally {
			l.unlock();
		}
	}

	private CmfStore<?> getStore(String name) {
		assertOpen();
		if (name == null) { throw new IllegalArgumentException("Must provide the name of the store to retrieve"); }
		Lock l = this.lock.readLock();
		l.lock();
		try {
			return this.cmfStores.get(name);
		} finally {
			l.unlock();
		}
	}

	private StoreConfiguration getConfiguration(String name) {
		assertOpen();
		if (name == null) {
			throw new IllegalArgumentException("Must provide the name of the store configuration to retrieve");
		}
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
				for (Map.Entry<String, CmfStore<?>> entry : this.cmfStores.entrySet()) {
					String n = entry.getKey();
					CmfStore<?> s = entry.getValue();
					try {
						s.close();
					} catch (Exception e) {
						this.log.warn("Exception caught closing CmfObjectStore {}[{}]", n,
							s.getClass().getCanonicalName(), e);
					}

					CmfStorePrep prep = this.cmfPreps.get(n);
					if (prep == null) {
						continue;
					}
					try {
						prep.close();
					} catch (Exception e) {
						this.log.warn("Exception caught cleaning up the CmfStorePrep for CmfObjectStore {}[{}]", n,
							s.getClass().getCanonicalName(), e);
					}
				}

				for (CmfStoreFactory<?> f : this.factories.values()) {
					try {
						f.close();
					} catch (Exception e) {
						this.log.warn("Exception caught closing Factory [{}]", f.getClass().getCanonicalName(), e);
					}
				}
			} finally {
				this.cmfStores.clear();
				this.cmfPreps.clear();
				this.factories.clear();
				this.configurations.clear();
				this.lock.writeLock().unlock();
			}
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(CmfStores.class);

	private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
	private static CmfStores OBJECT_STORES = null;
	private static CmfStores CONTENT_STORES = null;

	private static final Thread SHUTDOWN_HOOK = new Thread() {
		@Override
		public void run() {
			CmfStores.doClose();
		}
	};

	protected static StoreDefinitions parseConfiguration(File settings)
		throws CmfStorageException, IOException, JAXBException {
		if (settings == null) { throw new IllegalArgumentException("Must provide a file to read the settings from"); }
		return CmfStores.parseConfiguration(settings.toURI().toURL());
	}

	protected static StoreDefinitions parseConfiguration(URL settings)
		throws CmfStorageException, IOException, JAXBException {
		try (Reader xml = new InputStreamReader(settings.openStream())) {
			return CmfStores.parseConfiguration(xml);
		}
	}

	protected static StoreDefinitions parseConfiguration(Reader xml) throws CmfStorageException, JAXBException {
		return XmlTools.unmarshal(StoreDefinitions.class, "stores.xsd", xml);
	}

	public static void initialize(boolean configOnly) {
		CmfStores.LOCK.writeLock().lock();
		try {
			// If we're already initialized, dump out
			if ((CmfStores.OBJECT_STORES != null) || (CmfStores.CONTENT_STORES != null)) { return; }

			CmfStores.OBJECT_STORES = new CmfStores("Object", CmfObjectStoreFactory.class);
			CmfStores.CONTENT_STORES = new CmfStores("Content", CmfContentStoreFactory.class);

			Runtime.getRuntime().addShutdownHook(CmfStores.SHUTDOWN_HOOK);

			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			final Enumeration<URL> configs;
			try {
				configs = cl.getResources("META-INF/com/armedia/caliente/stores.xml");
			} catch (IOException e) {
				CmfStores.LOG.error("Failed to load the pre-defined content store configurations", e);
				return;
			}

			// We have the set of configurations, we iterate through each one
			while (configs.hasMoreElements()) {
				URL config = configs.nextElement();
				final StoreDefinitions cfg;
				try (Reader r = new InputStreamReader(config.openStream())) {
					cfg = CmfStores.parseConfiguration(r);
				} catch (Exception e) {
					String msg = String.format("Exception raised attempting to load the StoreDefinitions from [%s]",
						config);
					if (CmfStores.LOG.isDebugEnabled()) {
						CmfStores.LOG.warn(msg, e);
					} else {
						CmfStores.LOG.warn(msg);
					}
					continue;
				}

				if (configOnly) {
					CmfStores.OBJECT_STORES.initConfigurations(config, cfg.getObjectStores());
					CmfStores.CONTENT_STORES.initConfigurations(config, cfg.getContentStores());
				} else {
					CmfStores.OBJECT_STORES.initStores(config, cfg.getObjectStores());
					CmfStores.CONTENT_STORES.initStores(config, cfg.getContentStores());
				}
			}
		} finally {
			CmfStores.LOCK.writeLock().unlock();
		}
	}

	public static void initializeConfigurations() {
		CmfStores.initialize(true);
	}

	public static void initialize() {
		CmfStores.initialize(false);
	}

	private static boolean doClose() {
		CmfStores.LOCK.writeLock().lock();
		boolean ret = false;
		try {
			if (CmfStores.OBJECT_STORES != null) {
				CmfStores.OBJECT_STORES.closeInstance();
				CmfStores.OBJECT_STORES = null;
				ret = true;
			}
			if (CmfStores.CONTENT_STORES != null) {
				CmfStores.CONTENT_STORES.closeInstance();
				CmfStores.CONTENT_STORES = null;
				ret = true;
			}
			return ret;
		} finally {
			CmfStores.LOCK.writeLock().unlock();
		}
	}

	public static void close() {
		if (CmfStores.doClose()) {
			Runtime.getRuntime().removeShutdownHook(CmfStores.SHUTDOWN_HOOK);
		}
	}

	private static CmfStores assertValid(CmfStores instance) {
		if (instance == null) {
			throw new IllegalStateException("The requested CmfStores instance is no longer valid");
		}
		instance.assertOpen();
		return instance;
	}

	public static CmfObjectStore<?> createObjectStore(StoreConfiguration configuration)
		throws CmfStorageException, DuplicateCmfStoreException {
		CmfStores.initialize();
		CmfStores.LOCK.readLock().lock();
		try {
			return CmfObjectStore.class.cast(CmfStores.assertValid(CmfStores.OBJECT_STORES).createStore(configuration));
		} finally {
			CmfStores.LOCK.readLock().unlock();
		}
	}

	public static CmfContentStore<?, ?> createContentStore(StoreConfiguration configuration)
		throws CmfStorageException, DuplicateCmfStoreException {
		CmfStores.initialize();
		CmfStores.LOCK.readLock().lock();
		try {
			return CmfContentStore.class
				.cast(CmfStores.assertValid(CmfStores.CONTENT_STORES).createStore(configuration));
		} finally {
			CmfStores.LOCK.readLock().unlock();
		}
	}

	public static StoreConfiguration getObjectStoreConfiguration(String name) {
		CmfStores.initialize();
		CmfStores.LOCK.readLock().lock();
		try {
			return CmfStores.assertValid(CmfStores.OBJECT_STORES).getConfiguration(name);
		} finally {
			CmfStores.LOCK.readLock().unlock();
		}
	}

	public static CmfObjectStore<?> getObjectStore(String name) {
		CmfStores.initialize();
		CmfStores.LOCK.readLock().lock();
		try {
			return CmfObjectStore.class.cast(CmfStores.assertValid(CmfStores.OBJECT_STORES).getStore(name));
		} finally {
			CmfStores.LOCK.readLock().unlock();
		}
	}

	public static StoreConfiguration getContentStoreConfiguration(String name) {
		CmfStores.initialize();
		CmfStores.LOCK.readLock().lock();
		try {
			return CmfStores.assertValid(CmfStores.CONTENT_STORES).getConfiguration(name);
		} finally {
			CmfStores.LOCK.readLock().unlock();
		}
	}

	public static CmfContentStore<?, ?> getContentStore(String name) {
		CmfStores.initialize();
		CmfStores.LOCK.readLock().lock();
		try {
			return CmfContentStore.class.cast(CmfStores.assertValid(CmfStores.CONTENT_STORES).getStore(name));
		} finally {
			CmfStores.LOCK.readLock().unlock();
		}
	}
}
