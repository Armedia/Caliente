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

import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfOrganizationStrategy;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueSerializer;
import com.armedia.cmf.storage.local.xml.PropertyT;
import com.armedia.cmf.storage.local.xml.StorePropertiesT;
import com.armedia.cmf.storage.tools.FilenameFixer;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class LocalContentStore extends CmfContentStore<URI> {

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

		protected LocalHandle(CmfType objectType, String objectId, String qualifier, URI locator) {
			super(objectType, objectId, qualifier, locator);
		}

	}

	private static final String XML_SCHEMA = "store-properties.xsd";

	private final File baseDir;
	private final CmfOrganizationStrategy strategy;
	private final File propertiesFile;
	private final AtomicBoolean modified = new AtomicBoolean(false);
	private final CfgTools settings;
	private final Map<String, CmfValue> properties = new TreeMap<String, CmfValue>();
	private final boolean forceSafeFilenames;
	private final Charset safeFilenameEncoding;
	private final boolean fixFilenames;
	private final boolean failOnCollisions;
	private final boolean ignoreFragment;
	protected final boolean propertiesLoaded;
	private final boolean useWindowsFix;

	public LocalContentStore(CfgTools settings, File baseDir, CmfOrganizationStrategy strategy, boolean cleanData)
		throws CmfStorageException {
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
			CmfValue currentStrategyName = getProperty("strategy");
			if ((currentStrategyName != null) && !currentStrategyName.isNull()) {
				CmfOrganizationStrategy currentStrategy = CmfOrganizationStrategy.getStrategy(currentStrategyName
					.asString());
				if (currentStrategy != null) {
					strategy = currentStrategy;
					storeStrategyName = false;
				}
			}
		}
		this.strategy = strategy;
		if (this.strategy == null) { throw new IllegalArgumentException("Must provide a path strategy"); }
		if (storeStrategyName) {
			setProperty("strategy", new CmfValue(strategy.getName()));
		}

		// This seems clunky but it's actually very useful - it allows us to load properties
		// and apply them at constructor time in a consistent fashion...
		if (!this.propertiesLoaded) {
			initProperties();
		}

		CmfValue v = null;
		v = this.properties.get(Setting.FORCE_SAFE_FILENAMES.getLabel());
		this.forceSafeFilenames = ((v != null) && v.asBoolean());

		v = this.properties.get(Setting.SAFE_FILENAME_ENCODING.getLabel());
		if ((v != null) && this.forceSafeFilenames) {
			try {
				this.safeFilenameEncoding = Charset.forName(v.asString());
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Encoding [%s] is not supported", v.asString()), e);
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
		this.properties.put(Setting.USE_WINDOWS_FIX.getLabel(), new CmfValue(this.useWindowsFix));
	}

	protected void initProperties() throws CmfStorageException {
		final boolean forceSafeFilenames = this.settings.getBoolean(Setting.FORCE_SAFE_FILENAMES);
		final Charset safeFilenameEncoding;
		final boolean fixFilenames;
		if (forceSafeFilenames) {
			String encoding = this.settings.getString(Setting.SAFE_FILENAME_ENCODING);
			try {
				safeFilenameEncoding = Charset.forName(encoding);
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Encoding [%s] is not supported", encoding), e);
			}
			fixFilenames = false;
		} else {
			safeFilenameEncoding = null;
			fixFilenames = this.settings.getBoolean(Setting.FIX_FILENAMES);
		}
		final boolean failOnCollisions = this.settings.getBoolean(Setting.FAIL_ON_COLLISIONS);
		final boolean ignoreFragment = this.settings.getBoolean(Setting.IGNORE_EXTRA_FILENAME_INFO);
		final boolean useWindowsFix = this.settings.getBoolean(Setting.USE_WINDOWS_FIX);

		this.properties.put(Setting.FORCE_SAFE_FILENAMES.getLabel(), new CmfValue(forceSafeFilenames));
		if (safeFilenameEncoding != null) {
			this.properties.put(Setting.SAFE_FILENAME_ENCODING.getLabel(), new CmfValue(safeFilenameEncoding.name()));
		}
		this.properties.put(Setting.FIX_FILENAMES.getLabel(), new CmfValue(fixFilenames));
		this.properties.put(Setting.FAIL_ON_COLLISIONS.getLabel(), new CmfValue(failOnCollisions));
		this.properties.put(Setting.IGNORE_EXTRA_FILENAME_INFO.getLabel(), new CmfValue(ignoreFragment));
		this.properties.put(Setting.USE_WINDOWS_FIX.getLabel(),
			new CmfValue(useWindowsFix || SystemUtils.IS_OS_WINDOWS));
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
		if (this.fixFilenames) {
			str = FilenameFixer.safeEncode(str, this.useWindowsFix);
		}
		return str;
	}

	@Override
	protected URI doCalculateLocator(CmfAttributeTranslator<?> translator, CmfObject<?> object, String qualifier) {
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
		File[] files = this.baseDir.listFiles();
		if (files != null) {
			for (File f : files) {
				FileUtils.deleteQuietly(f);
			}
		}
	}

	protected synchronized boolean loadProperties() throws CmfStorageException {
		InputStream in = null;
		this.properties.clear();
		if (!this.propertiesFile.exists()) { return false; }
		// Allow an empty file...
		if (this.propertiesFile.length() == 0) { return true; }
		try {
			in = new FileInputStream(this.propertiesFile);
			StorePropertiesT p = XmlTools.unmarshal(StorePropertiesT.class, LocalContentStore.XML_SCHEMA, in);
			for (PropertyT property : p.getProperty()) {
				CmfValueSerializer deserializer = CmfValueSerializer.get(property.getType());
				if (deserializer == null) {
					continue;
				}
				final CmfValue v;
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
			throw new CmfStorageException("Failed to parse the stored properties", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	protected synchronized void storeProperties() throws CmfStorageException {
		OutputStream out = null;
		try {
			out = new FileOutputStream(this.propertiesFile);
			StorePropertiesT p = new StorePropertiesT();
			for (Map.Entry<String, CmfValue> e : this.properties.entrySet()) {
				final String n = e.getKey();
				final CmfValue v = e.getValue();
				CmfValueSerializer serializer = CmfValueSerializer.get(v.getDataType());
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
			throw new CmfStorageException("Failed to parse the store properties", e);
		} catch (IOException e) {
			throw new CmfStorageException("Failed to write the store properties", e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	protected CmfValue doGetProperty(String property) throws CmfStorageException {
		return this.properties.get(property);
	}

	@Override
	protected CmfValue doSetProperty(String property, CmfValue value) throws CmfStorageException {
		CmfValue ret = this.properties.put(property, value);
		this.modified.set(true);
		return ret;
	}

	@Override
	public Set<String> getPropertyNames() throws CmfStorageException {
		return new TreeSet<String>(this.properties.keySet());
	}

	@Override
	protected CmfValue doClearProperty(String property) throws CmfStorageException {
		CmfValue ret = this.properties.remove(property);
		this.modified.set(true);
		return ret;
	}

	@Override
	protected boolean doClose() {
		if (this.modified.get()) {
			try {
				storeProperties();
			} catch (CmfStorageException e) {
				this.log.error(String.format("Failed to write the store properties to [%s]",
					this.propertiesFile.getAbsolutePath()), e);
			}
		}
		return super.doClose();
	}

	@Override
	public void clearProperties() throws CmfStorageException {
		this.modified.set(true);
		this.properties.clear();
		this.propertiesFile.delete();
		this.modified.set(false);
	}

	@Override
	protected LocalHandle constructHandle(CmfObject<?> object, String qualifier, URI locator) {
		return new LocalHandle(object.getType(), object.getId(), qualifier, locator);
	}
}