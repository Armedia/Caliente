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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueSerializer;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.cmf.storage.local.xml.PropertyT;
import com.armedia.cmf.storage.local.xml.StorePropertiesT;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.XmlTools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class LocalContentStore extends ContentStore<URI> {

	// local:[/relative/path/to/file/within/the/target/directory]#fragment

	private class LocalHandle extends Handle {

		protected LocalHandle(StoredObjectType objectType, String objectId, String qualifier, URI locator) {
			super(objectType, objectId, qualifier, locator);
		}

	}

	private static final String SCHEME = "local";
	private static final String XML_SCHEMA = "store-properties.xsd";

	private final File baseDir;
	private final URIStrategy strategy;
	private final File propertiesFile;
	private final AtomicBoolean modified = new AtomicBoolean(false);
	private final CfgTools settings;
	private final Map<String, StoredValue> properties = new TreeMap<String, StoredValue>();
	private final boolean forceSafeFilenames;
	private final Charset safeFilenameEncoding;

	public LocalContentStore(CfgTools settings, File baseDir, URIStrategy strategy, boolean cleanData)
		throws StorageException {
		if (settings == null) { throw new IllegalArgumentException("Must provide configuration settings"); }
		if (baseDir == null) { throw new IllegalArgumentException("Must provide a base directory"); }
		if (baseDir.exists() && !baseDir.isDirectory()) { throw new IllegalArgumentException(String.format(
			"The file at [%s] is not a directory", baseDir.getAbsolutePath())); }
		if (!baseDir.exists() && !baseDir.mkdirs()) { throw new IllegalArgumentException(String.format(
			"Failed to create the full path at [%s] ", baseDir.getAbsolutePath())); }
		this.settings = settings;
		File f = baseDir;
		try {
			f = baseDir.getCanonicalFile();
		} catch (IOException e) {
			f = baseDir;
		}
		this.baseDir = f;
		this.propertiesFile = new File(baseDir, "store-properties.xml");
		loadProperties();
		boolean storeStrategyName = true;
		if (cleanData) {
			clearProperties();
			clearAllStreams();
		} else {
			StoredValue currentStrategyName = getProperty("strategy");
			if ((currentStrategyName != null) && !currentStrategyName.isNull()) {
				URIStrategy currentStrategy = URIStrategy.getStrategy(currentStrategyName.asString());
				if (currentStrategy != null) {
					strategy = currentStrategy;
					storeStrategyName = false;
				}
			}
		}
		this.strategy = strategy;
		if (this.strategy == null) { throw new IllegalArgumentException("Must provide a path strategy"); }
		if (storeStrategyName) {
			setProperty("strategy", new StoredValue(strategy.getName()));
		}
		this.forceSafeFilenames = this.settings.getBoolean(Setting.FORCE_SAFE_FILENAMES);
		if (this.forceSafeFilenames) {
			String encoding = this.settings.getString(Setting.SAFE_FILENAME_ENCODING);
			try {
				this.safeFilenameEncoding = Charset.forName(encoding);
			} catch (Exception e) {
				throw new StorageException(String.format("Encoding [%s] is not supported", encoding), e);
			}
		} else {
			this.safeFilenameEncoding = null;
		}
	}

	@Override
	protected boolean isSupported(URI locator) {
		return LocalContentStore.SCHEME.equals(locator.getScheme());
	}

	@Override
	protected URI doCalculateLocator(ObjectStorageTranslator<?> translator, StoredObject<?> object, String qualifier) {
		final String rawSSP = this.strategy.getSSP(translator, object);
		final String ssp;
		if (this.forceSafeFilenames) {
			List<String> sspParts = new ArrayList<String>();
			for (String s : FileNameTools.tokenize(rawSSP, '/')) {
				try {
					sspParts.add(URLEncoder.encode(s, this.safeFilenameEncoding.name()));
				} catch (UnsupportedEncodingException e) {
					// Not gonna happen...but still...better safe than sorry
					throw new RuntimeException(String.format("Encoding [%s] is not supported in this JVM",
						this.safeFilenameEncoding.name()), e);
				}
			}
			ssp = FileNameTools.reconstitute(sspParts, false, false, '/');
		} else {
			ssp = FileNameTools.reconstitute(FileNameTools.tokenize(rawSSP, '/'), false, false, '/');
		}

		final String fragment = this.strategy.calculateFragment(translator, object, qualifier);
		try {
			return new URI(LocalContentStore.SCHEME, ssp, fragment);
		} catch (URISyntaxException e) {
			throw new RuntimeException(String.format("Failed to allocate a handle ID for %s[%s]", object.getType(),
				object.getId()), e);
		}
	}

	@Override
	protected final File doGetFile(URI locator) {
		String ssp = locator.getSchemeSpecificPart();
		String frag = locator.getFragment();
		String path = ssp;
		if (!StringUtils.isBlank(frag)) {
			path = (frag != null ? String.format("%s#%s", ssp, frag) : ssp);
		}
		return new File(this.baseDir, path);
	}

	@Override
	protected InputStream doOpenInput(URI locator) throws IOException {
		return new FileInputStream(getFile(locator));
	}

	@Override
	protected OutputStream doOpenOutput(URI locator) throws IOException {
		File f = getFile(locator);
		f.getParentFile().mkdirs(); // Create the parents, if needed
		if (f.createNewFile() || f.exists()) { return new FileOutputStream(f); }
		throw new IOException(String.format("Failed to create the non-existent target file [%s]", f.getAbsolutePath()));
	}

	@Override
	protected boolean doIsExists(URI locator) {
		return getFile(locator).exists();
	}

	@Override
	protected long doGetStreamSize(URI locator) {
		File f = getFile(locator);
		return (f.exists() ? f.length() : -1);
	}

	@Override
	protected boolean isSupportsFileAccess() {
		return true;
	}

	@Override
	protected void doClearAllStreams() {
		for (File f : this.baseDir.listFiles()) {
			FileUtils.deleteQuietly(f);
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

	@Override
	public void clearProperties() throws StorageException {
		this.modified.set(true);
		this.properties.clear();
		this.propertiesFile.delete();
		this.modified.set(false);
	}

	@Override
	protected LocalHandle constructHandle(StoredObject<?> object, String qualifier, URI locator) {
		return new LocalHandle(object.getType(), object.getId(), qualifier, locator);
	}
}