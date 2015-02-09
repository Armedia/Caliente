/**
 *
 */

package com.armedia.cmf.storage.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueSerializer;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.cmf.storage.local.xml.PropertyT;
import com.armedia.cmf.storage.local.xml.StorePropertiesT;
import com.armedia.commons.utilities.XmlTools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class LocalContentStore extends ContentStore {

	private static final String SCHEME = "local";
	private static final String XML_SCHEMA = "store-properties.xsd";

	private final File baseDir;
	private final URIStrategy strategy;
	private final File propertiesFile;
	private final AtomicBoolean modified = new AtomicBoolean(false);
	private final Map<String, StoredValue> properties = new TreeMap<String, StoredValue>();

	public LocalContentStore(File baseDir, URIStrategy strategy) throws StorageException {
		if (baseDir == null) { throw new IllegalArgumentException("Must provide a base directory"); }
		if (baseDir.exists() && !baseDir.isDirectory()) { throw new IllegalArgumentException(String.format(
			"The file at [%s] is not a directory", baseDir.getAbsolutePath())); }
		if (!baseDir.exists() && !baseDir.mkdirs()) { throw new IllegalArgumentException(String.format(
			"Failed to create the full path at [%s] ", baseDir.getAbsolutePath())); }
		File f = baseDir;
		try {
			f = baseDir.getCanonicalFile();
		} catch (IOException e) {
			f = baseDir;
		}
		this.baseDir = f;
		this.propertiesFile = new File(baseDir, "store-properties.xml");
		loadProperties();
		StoredValue currentStrategyName = getProperty("strategy");
		boolean storeStrategyName = true;
		if ((currentStrategyName != null) && !currentStrategyName.isNull()) {
			URIStrategy currentStrategy = URIStrategy.getStrategy(currentStrategyName.asString());
			if (currentStrategy != null) {
				strategy = currentStrategy;
				storeStrategyName = false;
			}
		}
		this.strategy = strategy;
		if (this.strategy == null) { throw new IllegalArgumentException("Must provide a path strategy"); }
		if (storeStrategyName) {
			setProperty("strategy", new StoredValue(strategy.getName()));
		}
	}

	@Override
	protected boolean isSupportedURI(URI uri) {
		return LocalContentStore.SCHEME.equals(uri.getScheme());
	}

	@Override
	protected URI doAllocateHandleId(StoredObject<?> object, String qualifier) {
		try {
			return new URI(LocalContentStore.SCHEME, this.strategy.getSSP(object), this.strategy.calculateFragment(
				object, qualifier));
		} catch (URISyntaxException e) {
			throw new RuntimeException(String.format("Failed to allocate a handle ID for %s[%s]", object.getType(),
				object.getId()), e);
		}
	}

	@Override
	protected final File doGetFile(URI handleId) {
		String ssp = handleId.getSchemeSpecificPart();
		String frag = handleId.getFragment();
		String path = (frag != null ? String.format("%s%s", ssp, frag) : ssp);
		return new File(this.baseDir, path);
	}

	@Override
	protected InputStream doOpenInput(URI handleId) throws IOException {
		return new FileInputStream(getFile(handleId));
	}

	@Override
	protected OutputStream doOpenOutput(URI handleId) throws IOException {
		File f = getFile(handleId);
		f.getParentFile().mkdirs(); // Create the parents, if needed
		if (f.createNewFile() || f.exists()) { return new FileOutputStream(f); }
		throw new IOException(String.format("Failed to create the non-existent target file [%s]", f.getAbsolutePath()));
	}

	@Override
	protected boolean doIsExists(URI handleId) {
		return getFile(handleId).exists();
	}

	@Override
	protected long doGetStreamSize(URI handleId) {
		File f = getFile(handleId);
		return (f.exists() ? f.length() : -1);
	}

	@Override
	protected boolean isSupportsFileAccess() {
		return true;
	}

	@Override
	protected void doClearAllStreams() {
		for (File f : this.baseDir.listFiles()) {
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				// Ignore it, keep going
			}
		}
	}

	protected synchronized void loadProperties() throws StorageException {
		InputStream in = null;
		this.properties.clear();
		// Allow an empty file...
		if (!this.propertiesFile.exists() || (this.propertiesFile.length() == 0)) { return; }
		try {
			in = new FileInputStream(this.propertiesFile);
			StorePropertiesT p = XmlTools.unmarshal(StorePropertiesT.class, LocalContentStore.XML_SCHEMA, in);
			for (PropertyT property : p.getProperty()) {
				StoredValueSerializer deserializer = StoredValueSerializer.get(property.getType());
				if (deserializer == null) {
					continue;
				}
				final StoredValue v;
				try {
					v = deserializer.deserialize(property.getValue());
				} catch (Exception e) {
					this.log.warn(String.format(
						"Failed to deserialize the value for store property [%s]:  [%s] not valid as a [%s]",
						property.getName(), property.getValue(), property.getType()));
					continue;
				}
				if ((v != null) && !v.isNull()) {
					this.properties.put(property.getName(), v);
				}
			}
		} catch (FileNotFoundException e) {
			return;
		} catch (JAXBException e) {
			throw new StorageException("Failed to parse the stored properties", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	protected synchronized void storeProperties() throws StorageException {
		OutputStream out = null;
		try {
			out = new FileOutputStream(this.propertiesFile);
			StorePropertiesT p = new StorePropertiesT();
			for (Map.Entry<String, StoredValue> e : this.properties.entrySet()) {
				final String n = e.getKey();
				final StoredValue v = e.getValue();
				StoredValueSerializer serializer = StoredValueSerializer.get(v.getDataType());
				if (serializer == null) {
					continue;
				}
				PropertyT property = new PropertyT();
				property.setName(n);
				property.setType(v.getDataType());
				try {
					property.setValue(serializer.serialize(v));
				} catch (Exception ex) {
					this.log.warn(String.format("Failed to serialize the value for store property [%s]:  [%s]", n, v));
					continue;
				}
				p.getProperty().add(property);
			}
			XmlTools.marshal(p, LocalContentStore.XML_SCHEMA, out, true);
			out.flush();
		} catch (FileNotFoundException e) {
			return;
		} catch (JAXBException e) {
			throw new StorageException("Failed to parse the store properties", e);
		} catch (IOException e) {
			throw new StorageException("Failed to write the store properties", e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	protected StoredValue doGetProperty(String property) throws StorageException {
		return this.properties.get(property);
	}

	@Override
	protected StoredValue doSetProperty(String property, StoredValue value) throws StorageException {
		StoredValue ret = this.properties.put(property, value);
		this.modified.set(true);
		return ret;
	}

	@Override
	public Set<String> getPropertyNames() throws StorageException {
		return new TreeSet<String>(this.properties.keySet());
	}

	@Override
	protected StoredValue doClearProperty(String property) throws StorageException {
		StoredValue ret = this.properties.remove(property);
		this.modified.set(true);
		return ret;
	}

	@Override
	protected boolean doClose() {
		if (this.modified.get()) {
			try {
				storeProperties();
			} catch (StorageException e) {
				this.log.error(String.format("Failed to write the store properties to [%s]",
					this.propertiesFile.getAbsolutePath()), e);
			}
		}
		return super.doClose();
	}
}