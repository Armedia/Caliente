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

import com.armedia.commons.utilities.XmlTools;

public abstract class CmsObjectStoreFactory {

	private final Class<? extends CmsObjectStore> storeClass;

	private interface Cfg {

	}

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
			String key = f.getObjectStoreClass().getCanonicalName();
			if (m.containsKey(key)) { throw new RuntimeException(String.format(
				"Duplicate factories found with class name [%s]", key)); }
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
				// TODO: Parse the XML into the configuration object
				Reader r = null;
				try {
					r = new InputStreamReader(config.openStream());
				} catch (IOException e) {
					// Error - report it, don't die
				} finally {
					IOUtils.closeQuietly(r);
				}

				try {
					CmsObjectStoreFactory.createInstance(r);
				} catch (Throwable t) {
					// Log, but do not croak
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to load the pre-defined object store configurations"), e);
		}
	}

	public static <T extends CmsObjectStore> T createInstance(File settings) throws CmsStorageException,
		CmsDuplicateObjectStoreException, IOException {
		if (settings == null) { throw new IllegalArgumentException("Must provide a file to read the settings from"); }
		return CmsObjectStoreFactory.createInstance(settings.toURI().toURL());
	}

	public static <T extends CmsObjectStore> T createInstance(URL settings) throws CmsStorageException,
		CmsDuplicateObjectStoreException, IOException {
		Reader xml = null;
		try {
			xml = new InputStreamReader(settings.openStream());
			return CmsObjectStoreFactory.createInstance(xml);
		} finally {
			IOUtils.closeQuietly(xml);
		}
	}

	public static <T extends CmsObjectStore> T createInstance(Reader xml) throws CmsStorageException,
	CmsDuplicateObjectStoreException {
		try {
			return CmsObjectStoreFactory.createInstance(XmlTools.unmarshal(Cfg.class, "objectstore.xsd", xml));
		} catch (JAXBException e) {
			throw new CmsStorageException("Failed to parse the XML configuration", e);
		}
	}

	public static <T extends CmsObjectStore> T createInstance(Cfg configuration) throws CmsStorageException,
		CmsDuplicateObjectStoreException {
		Lock l = CmsObjectStoreFactory.STORE_LOCK.writeLock();
		l.lock();
		try {
			return null;
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
}