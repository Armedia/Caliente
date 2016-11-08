/**
 *
 */

package com.armedia.caliente.store.local;

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

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfOrganizationStrategy;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.caliente.store.CmfOrganizationStrategy.Location;
import com.armedia.caliente.store.local.xml.PropertyT;
import com.armedia.caliente.store.local.xml.StorePropertiesT;
import com.armedia.caliente.store.tools.FilenameFixer;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class LocalContentStore extends CmfContentStore<URI, File, LocalStoreOperation> {

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

		protected LocalHandle(CmfObject<?> object, CmfContentInfo info, URI locator) {
			super(object, info, locator);
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
	private final boolean ignoreDescriptor;
	protected final boolean propertiesLoaded;
	private final boolean useWindowsFix;

	public LocalContentStore(CfgTools settings, File baseDir, CmfOrganizationStrategy strategy, boolean cleanData)
		throws CmfStorageException {
		if (settings == null) { throw new IllegalArgumentException("Must provide configuration settings"); }
		if (baseDir == null) { throw new IllegalArgumentException("Must provide a base directory"); }
		if (baseDir.exists() && !baseDir.isDirectory()) { throw new IllegalArgumentException(
			String.format("The file at [%s] is not a directory", baseDir.getAbsolutePath())); }
		if (!baseDir.exists() && !baseDir.mkdirs()) { throw new IllegalArgumentException(
			String.format("Failed to create the full path at [%s] ", baseDir.getAbsolutePath())); }
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
				CmfOrganizationStrategy currentStrategy = CmfOrganizationStrategy
					.getStrategy(currentStrategyName.asString());
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

		v = this.properties.get(Setting.IGNORE_DESCRIPTOR.getLabel());
		this.ignoreDescriptor = ((v != null) && v.asBoolean());

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
		final boolean ignoreFragment = this.settings.getBoolean(Setting.IGNORE_DESCRIPTOR);
		final boolean useWindowsFix = this.settings.getBoolean(Setting.USE_WINDOWS_FIX);

		this.properties.put(Setting.FORCE_SAFE_FILENAMES.getLabel(), new CmfValue(forceSafeFilenames));
		if (safeFilenameEncoding != null) {
			this.properties.put(Setting.SAFE_FILENAME_ENCODING.getLabel(), new CmfValue(safeFilenameEncoding.name()));
		}
		this.properties.put(Setting.FIX_FILENAMES.getLabel(), new CmfValue(fixFilenames));
		this.properties.put(Setting.FAIL_ON_COLLISIONS.getLabel(), new CmfValue(failOnCollisions));
		this.properties.put(Setting.IGNORE_DESCRIPTOR.getLabel(), new CmfValue(ignoreFragment));
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
				throw new RuntimeException(
					String.format("Encoding [%s] is not supported in this JVM", this.safeFilenameEncoding.name()), e);
			}
		}
		if (this.fixFilenames) {
			str = FilenameFixer.safeEncode(str, this.useWindowsFix);
		}
		return str;
	}

	private <T> String constructFileName(Location loc) {
		String baseName = loc.baseName;
		String descriptor = (this.ignoreDescriptor ? "" : loc.descriptor);
		String ext = loc.extension;
		String appendix = loc.appendix;

		if (StringUtils.isEmpty(baseName)) {
			baseName = "";
		}

		if (!StringUtils.isEmpty(ext)) {
			ext = String.format(".%s", ext);
		} else {
			ext = "";
		}

		if (!StringUtils.isEmpty(ext) && baseName.endsWith(ext)) {
			// Remove the extension so it's the last thing on the filename
			baseName = baseName.substring(0, baseName.length() - ext.length());
			if (StringUtils.isEmpty(baseName) && !this.ignoreDescriptor) {
				baseName = "$CMF$";
			}
		}

		if (!StringUtils.isEmpty(descriptor)) {
			descriptor = String.format("[%s]", descriptor);
		} else {
			descriptor = "";
		}

		if (!StringUtils.isEmpty(appendix)) {
			appendix = String.format(".%s", appendix);
		} else {
			appendix = "";
		}

		return String.format("%s%s%s%s", baseName, descriptor, ext, appendix);
	}

	@Override
	protected <T> URI doCalculateLocator(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		final Location location = this.strategy.getLocation(translator, object, info);
		final List<String> rawPath = new ArrayList<String>(location.containerSpec);
		rawPath.add(constructFileName(location));

		final String scheme;
		final String ssp;
		if (this.forceSafeFilenames || this.fixFilenames) {
			boolean fixed = false;
			List<String> sspParts = new ArrayList<String>();
			for (String s : rawPath) {
				String S = safeEncode(s);
				fixed |= !Tools.equals(s, S);
				sspParts.add(S);
			}
			ssp = FileNameTools.reconstitute(sspParts, false, false, '/');

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
		}

		try {
			URI uri = new URI(scheme, ssp, null);
			this.log.info(String.format("Generated URI %s", uri));
			return uri;
		} catch (URISyntaxException e) {
			throw new RuntimeException(
				String.format("Failed to allocate a handle ID for %s[%s]", object.getType(), object.getId()), e);
		}
	}

	@Override
	protected final File doGetFile(URI locator) {
		return new File(this.baseDir, locator.getSchemeSpecificPart());
	}

	@Override
	protected InputStream openInput(LocalStoreOperation op, URI locator) throws CmfStorageException {
		final File f;
		try {
			f = getFile(locator);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to identify the file for [%s]", locator), e);
		}
		try {
			return new FileInputStream(f);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to open the file at [%s] for input", f), e);
		}
	}

	@Override
	protected long setContents(LocalStoreOperation op, URI locator, InputStream in) throws CmfStorageException {
		final File f;
		try {
			f = getFile(locator);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to identify the file for [%s]", locator), e);
		}
		f.getParentFile().mkdirs(); // Create the parents, if needed

		boolean created;
		try {
			created = f.createNewFile();
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to create the file at [%s]", f), e);
		}
		if (!created) {
			if (this.failOnCollisions) { throw new CmfStorageException(String.format(
				"Filename collision detected for target file [%s] - a file already exists at that location",
				f.getAbsolutePath())); }
			if (!f.exists()) { throw new CmfStorageException(
				String.format("Failed to create the non-existent target file [%s]", f.getAbsolutePath())); }
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			throw new CmfStorageException(String.format("Failed to open the output stream to the file at [%s]", f), e);
		}
		try {
			IOUtils.copyLarge(in, out);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to write the content out to the file at [%s]", f), e);
		} finally {
			IOUtils.closeQuietly(out);
		}
		return f.length();
	}

	@Override
	protected boolean isExists(LocalStoreOperation op, URI locator) throws CmfStorageException {
		final File f;
		try {
			f = getFile(locator);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to identify the file for [%s]", locator), e);
		}
		return f.exists();
	}

	@Override
	protected long getStreamSize(LocalStoreOperation op, URI locator) throws CmfStorageException {
		final File f;
		try {
			f = getFile(locator);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to identify the file for [%s]", locator), e);
		}
		return (f.exists() ? f.length() : -1);
	}

	@Override
	public boolean isSupportsFileAccess() {
		return true;
	}

	@Override
	protected File doGetRootLocation() {
		return this.baseDir;
	}

	@Override
	protected void clearAllStreams(LocalStoreOperation op) {
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
	protected LocalStoreOperation newOperation() throws CmfStorageException {
		return new LocalStoreOperation(this.baseDir);
	}

	@Override
	protected void clearProperties(LocalStoreOperation operation) throws CmfStorageException {
		this.modified.set(true);
		this.properties.clear();
		this.propertiesFile.delete();
		this.modified.set(false);
	}

	@Override
	protected CmfValue getProperty(LocalStoreOperation operation, String property) throws CmfStorageException {
		return this.properties.get(property);
	}

	@Override
	protected CmfValue setProperty(LocalStoreOperation operation, String property, CmfValue value)
		throws CmfStorageException {
		CmfValue ret = this.properties.put(property, value);
		this.modified.set(true);
		return ret;
	}

	@Override
	protected Set<String> getPropertyNames(LocalStoreOperation operation) throws CmfStorageException {
		return new TreeSet<String>(this.properties.keySet());
	}

	@Override
	protected CmfValue clearProperty(LocalStoreOperation operation, String property) throws CmfStorageException {
		CmfValue ret = this.properties.remove(property);
		this.modified.set(true);
		return ret;
	}

	@Override
	protected boolean doClose(boolean cleanupIfEmpty) {
		if (this.modified.get()) {
			try {
				storeProperties();
			} catch (CmfStorageException e) {
				this.log.error(String.format("Failed to write the store properties to [%s]",
					this.propertiesFile.getAbsolutePath()), e);
			}
		}
		return super.doClose(cleanupIfEmpty);
	}

	@Override
	protected LocalHandle constructHandle(CmfObject<?> object, CmfContentInfo info, URI locator) {
		return new LocalHandle(object, info, locator);
	}
}