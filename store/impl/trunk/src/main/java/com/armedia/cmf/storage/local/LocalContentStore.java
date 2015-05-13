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
import java.util.HashSet;
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
import org.apache.commons.lang3.SystemUtils;

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
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class LocalContentStore extends ContentStore<URI> {

	private static final String SCHEME_RAW = "raw";
	private static final String SCHEME_FIXED = "fixed";
	private static final String SCHEME_SAFE = "safe";

	private static final Set<String> SUPPORTED;
	static {
		Set<String> s = new HashSet<String>();
		s.add(LocalContentStore.SCHEME_RAW);
		s.add(LocalContentStore.SCHEME_FIXED);
		s.add(LocalContentStore.SCHEME_SAFE);
		SUPPORTED = Tools.freezeSet(s);
	}

	private class LocalHandle extends Handle {

		protected LocalHandle(StoredObjectType objectType, String objectId, String qualifier, URI locator) {
			super(objectType, objectId, qualifier, locator);
		}

	}

	private static final String XML_SCHEMA = "store-properties.xsd";

	private final File baseDir;
	private final URIStrategy strategy;
	private final File propertiesFile;
	private final AtomicBoolean modified = new AtomicBoolean(false);
	private final CfgTools settings;
	private final Map<String, StoredValue> properties = new TreeMap<String, StoredValue>();
	private final boolean forceSafeFilenames;
	private final Charset safeFilenameEncoding;
	private final boolean fixFilenames;
	private final boolean failOnCollisions;
	private final boolean ignoreFragment;
	protected final boolean propertiesLoaded;
	private final boolean useWindowsFix;

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
		boolean storeStrategyName = true;
		if (cleanData) {
			this.propertiesLoaded = false;
			clearProperties();
			clearAllStreams();
		} else {
			this.propertiesLoaded = loadProperties();
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

		// This seems clunky but it's actually very useful - it allows us to load properties
		// and apply them at constructor time in a consistent fashion...
		if (!this.propertiesLoaded) {
			initProperties();
		}

		StoredValue v = null;
		v = this.properties.get(Setting.FORCE_SAFE_FILENAMES.getLabel());
		this.forceSafeFilenames = ((v != null) && v.asBoolean());

		v = this.properties.get(Setting.SAFE_FILENAME_ENCODING.getLabel());
		if ((v != null) && this.forceSafeFilenames) {
			try {
				this.safeFilenameEncoding = Charset.forName(v.asString());
			} catch (Exception e) {
				throw new StorageException(String.format("Encoding [%s] is not supported", v.asString()), e);
			}
			this.fixFilenames = false;
		} else {
			this.safeFilenameEncoding = null;

			v = this.properties.get(Setting.FIX_FILENAMES.getLabel());
			this.fixFilenames = ((v != null) && v.asBoolean());
		}

		v = this.properties.get(Setting.FAIL_ON_COLLISIONS.getLabel());
		this.failOnCollisions = ((v != null) && v.asBoolean());

		v = this.properties.get(Setting.IGNORE_EXTRA_FILENAME_INFO.getLabel());
		this.ignoreFragment = ((v != null) && v.asBoolean());

		v = this.properties.get(Setting.USE_WINDOWS_FIX.getLabel());
		this.useWindowsFix = ((v != null) && v.asBoolean());
		// This helps make sure the actual used value is stored
		this.properties.put(Setting.USE_WINDOWS_FIX.getLabel(), new StoredValue(this.useWindowsFix));
	}

	protected void initProperties() throws StorageException {
		final boolean forceSafeFilenames = this.settings.getBoolean(Setting.FORCE_SAFE_FILENAMES);
		final Charset safeFilenameEncoding;
		final boolean fixFilenames;
		if (forceSafeFilenames) {
			String encoding = this.settings.getString(Setting.SAFE_FILENAME_ENCODING);
			try {
				safeFilenameEncoding = Charset.forName(encoding);
			} catch (Exception e) {
				throw new StorageException(String.format("Encoding [%s] is not supported", encoding), e);
			}
			fixFilenames = false;
		} else {
			safeFilenameEncoding = null;
			fixFilenames = this.settings.getBoolean(Setting.FIX_FILENAMES);
		}
		final boolean failOnCollisions = this.settings.getBoolean(Setting.FAIL_ON_COLLISIONS);
		final boolean ignoreFragment = this.settings.getBoolean(Setting.IGNORE_EXTRA_FILENAME_INFO);
		final boolean useWindowsFix = this.settings.getBoolean(Setting.USE_WINDOWS_FIX);

		this.properties.put(Setting.FORCE_SAFE_FILENAMES.getLabel(), new StoredValue(forceSafeFilenames));
		if (safeFilenameEncoding != null) {
			this.properties
				.put(Setting.SAFE_FILENAME_ENCODING.getLabel(), new StoredValue(safeFilenameEncoding.name()));
		}
		this.properties.put(Setting.FIX_FILENAMES.getLabel(), new StoredValue(fixFilenames));
		this.properties.put(Setting.FAIL_ON_COLLISIONS.getLabel(), new StoredValue(failOnCollisions));
		this.properties.put(Setting.IGNORE_EXTRA_FILENAME_INFO.getLabel(), new StoredValue(ignoreFragment));
		this.properties.put(Setting.USE_WINDOWS_FIX.getLabel(), new StoredValue(useWindowsFix
			|| SystemUtils.IS_OS_WINDOWS));
	}

	@Override
	protected boolean isSupported(URI locator) {
		return LocalContentStore.SUPPORTED.contains(locator.getScheme());
	}

	protected String safeEncode(String str) {
		if (this.forceSafeFilenames) {
			if (this.safeFilenameEncoding == null) { return str; }
			try {
				return URLEncoder.encode(str, this.safeFilenameEncoding.name());
			} catch (UnsupportedEncodingException e) {
				// Not gonna happen...but still...better safe than sorry
				throw new RuntimeException(String.format("Encoding [%s] is not supported in this JVM",
					this.safeFilenameEncoding.name()), e);
			}
		}
		if (str.startsWith("File With") || str.startsWith("Filename")) {
			str.hashCode();
		}
		if (this.fixFilenames) {
			// This covers Unix...
			str = str.replace('\0', '_');
			str = str.replace('/', '_');

			if (this.useWindowsFix) {
				// Now comes the fun part - Windows...only do this for windows!!
				str = str.replace('<', '_');
				str = str.replace('>', '_');
				str = str.replace(':', '_');
				str = str.replace('\\', '_');
				str = str.replace('|', '_');
				str = str.replace('?', '_');
				str = str.replace('*', '_');
				for (int i = 1; i <= 31; i++) {
					str = str.replace((char) i, '_');
				}

				str = str.replaceAll("(\\.+)$", "$1_"); // Can't end with a dot
				str = str.replaceAll("(\\\\s+)$", "$1_"); // Can't end with a space
				str = str.replaceAll("^(\\\\s+)", "_$1"); // Can't begin with a space

				// Also invalid are CON, PRN, AUX, NUL, COM[1-9], LPT[1-9], CLOCK$, but we can't
				// fix those so we just leave them alone and let the OS failure take its course
			}
		}
		return str;
	}

	@Override
	protected URI doCalculateLocator(ObjectStorageTranslator<?> translator, StoredObject<?> object, String qualifier) {
		final List<String> rawPath = this.strategy.getPath(translator, object);
		final String rawFragment;
		if (!this.ignoreFragment) {
			rawFragment = this.strategy.calculateAddendum(translator, object, qualifier);
		} else {
			rawFragment = "";
		}
		final String ssp;
		final String fragment;
		final String scheme;
		if (this.forceSafeFilenames || this.fixFilenames) {
			boolean fixed = false;
			List<String> sspParts = new ArrayList<String>();
			for (String s : rawPath) {
				String S = safeEncode(s);
				fixed = fixed || !Tools.equals(s, S);
				sspParts.add(S);
			}
			ssp = FileNameTools.reconstitute(sspParts, false, false, '/');
			fragment = safeEncode(rawFragment);
			fixed = fixed || !Tools.equals(rawFragment, fragment);
			if (fixed) {
				if (this.forceSafeFilenames) {
					scheme = LocalContentStore.SCHEME_SAFE;
				} else {
					scheme = LocalContentStore.SCHEME_FIXED;
				}
			} else {
				scheme = LocalContentStore.SCHEME_RAW;
			}
		} else {
			scheme = LocalContentStore.SCHEME_RAW;
			ssp = FileNameTools.reconstitute(rawPath, false, false, '/');
			fragment = rawFragment;
		}

		try {
			return new URI(scheme, ssp, fragment);
		} catch (URISyntaxException e) {
			throw new RuntimeException(String.format("Failed to allocate a handle ID for %s[%s]", object.getType(),
				object.getId()), e);
		}
	}

	@Override
	protected final File doGetFile(URI locator) {
		String ssp = locator.getSchemeSpecificPart();
		String frag = (!this.ignoreFragment ? locator.getFragment() : "");
		String path = ssp;
		if (!this.ignoreFragment && !StringUtils.isBlank(frag)) {
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

		final boolean created = f.createNewFile();
		if (!created) {
			if (this.failOnCollisions) { throw new IOException(String.format(
				"Filename collision detected for target file [%s] - a file already exists at that location",
				f.getAbsolutePath())); }
			if (!f.exists()) { throw new IOException(String.format(
				"Failed to create the non-existent target file [%s]", f.getAbsolutePath())); }
		}
		return new FileOutputStream(f);
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

	protected synchronized boolean loadProperties() throws StorageException {
		InputStream in = null;
		this.properties.clear();
		if (!this.propertiesFile.exists()) { return false; }
		// Allow an empty file...
		if (this.propertiesFile.length() == 0) { return true; }
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
			return true;
		} catch (FileNotFoundException e) {
			return false;
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