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
import com.armedia.caliente.store.CmfContentOrganizer;
import com.armedia.caliente.store.CmfContentOrganizer.Location;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.caliente.store.local.xml.PropertiesLoader;
import com.armedia.caliente.store.local.xml.PropertyT;
import com.armedia.caliente.store.local.xml.StorePropertiesT;
import com.armedia.caliente.store.local.xml.legacy.LegacyPropertiesLoader;
import com.armedia.caliente.store.tools.FilenameEncoder;
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
		Set<String> s = new HashSet<>();
		s.add(LocalContentStore.SCHEME_RAW);
		s.add(LocalContentStore.SCHEME_FIXED);
		s.add(LocalContentStore.SCHEME_SAFE);
		SUPPORTED = Tools.freezeSet(s);
	}

	private class LocalHandle extends Handle {

		protected LocalHandle(CmfObject<?> object, CmfContentStream info, URI locator) {
			super(object, info, locator);
		}

	}

	private final File baseDir;
	private final boolean storeProperties;
	private final CmfContentOrganizer organizer;
	private final File propertiesFile;
	private final AtomicBoolean modified = new AtomicBoolean(false);
	private final CfgTools settings;
	private final Map<String, CmfValue> properties = new TreeMap<>();
	private final boolean forceSafeFilenames;
	private final Charset safeFilenameEncoding;
	private final boolean fixFilenames;
	private final boolean failOnCollisions;
	private final boolean ignoreDescriptor;
	protected final boolean propertiesLoaded;
	private final boolean useWindowsFix;

	private static boolean fileIsAccessible(File f) {
		return f.exists() && f.isFile() && f.canRead();
	}

	public LocalContentStore(CfgTools settings, File baseDir, CmfContentOrganizer organizer, boolean cleanData)
		throws CmfStorageException {
		if (settings == null) { throw new IllegalArgumentException("Must provide configuration settings"); }
		if (baseDir == null) { throw new IllegalArgumentException("Must provide a base directory"); }
		if (baseDir.exists() && !baseDir.isDirectory()) {
			throw new IllegalArgumentException(
				String.format("The file at [%s] is not a directory", baseDir.getAbsolutePath()));
		}
		if (!baseDir.exists() && !baseDir.mkdirs()) {
			throw new IllegalArgumentException(
				String.format("Failed to create the full path at [%s] ", baseDir.getAbsolutePath()));
		}
		this.settings = settings;
		File f = baseDir;
		try {
			f = baseDir.getCanonicalFile();
		} catch (IOException e) {
			f = baseDir;
		}
		this.baseDir = f;
		this.storeProperties = settings.getBoolean(LocalContentStoreSetting.STORE_PROPERTIES);

		final File newPropertiesFile = new File(baseDir, "caliente-store-properties.xml");
		final File oldPropertiesFile = new File(baseDir, "store-properties.xml");
		if (LocalContentStore.fileIsAccessible(newPropertiesFile)
			|| !LocalContentStore.fileIsAccessible(oldPropertiesFile)) {
			// If the new format existsneither exists, or neither exists,
			// then we go with the new filename format
			this.propertiesFile = newPropertiesFile;
		} else {
			// If the old format exists, then we use it as-is
			this.propertiesFile = oldPropertiesFile;
		}

		boolean storeOrganizerName = true;
		if (cleanData) {
			this.propertiesLoaded = false;
			clearProperties();
			clearAllStreams();
		} else {
			// First, try the legacy mode...
			boolean propertiesLoaded = false;
			try {
				propertiesLoaded = new PropertiesLoader().loadProperties(this.propertiesFile, this.properties);
			} catch (CmfStorageException e) {
				// Legacy didn't work....log a warning?
				this.log.warn("Failed to load the store properties, will try the legacy model");
				try {
					propertiesLoaded = new LegacyPropertiesLoader().loadProperties(this.propertiesFile,
						this.properties);
				} catch (CmfStorageException e2) {
					this.log.error("Failed to load the store properties using the legacy model", e2);
					throw new CmfStorageException("Failed to load both the modern and legacy properties models", e);
				}
			}

			this.propertiesLoaded = propertiesLoaded;

			CmfValue currentOrganizerName = getProperty("organizer");
			if ((currentOrganizerName == null) || currentOrganizerName.isNull()) {
				// For backwards compatibility
				currentOrganizerName = getProperty("strategy");
			}
			if ((currentOrganizerName != null) && !currentOrganizerName.isNull()) {
				CmfContentOrganizer currentOrganizer = CmfContentOrganizer
					.getOrganizer(currentOrganizerName.asString());
				if (currentOrganizer != null) {
					organizer = currentOrganizer;
					storeOrganizerName = false;
				}
			}
		}
		this.organizer = organizer;
		if (this.organizer == null) { throw new IllegalArgumentException("Must provide a content organizer"); }
		if (storeOrganizerName) {
			setProperty("organizer", new CmfValue(organizer.getName()));
		}

		// This seems clunky but it's actually very useful - it allows us to load properties
		// and apply them at constructor time in a consistent fashion...
		if (!this.propertiesLoaded) {
			initProperties();
		}

		CmfValue v = null;
		v = this.properties.get(LocalContentStoreSetting.FORCE_SAFE_FILENAMES.getLabel());
		this.forceSafeFilenames = ((v != null) && v.asBoolean());

		v = this.properties.get(LocalContentStoreSetting.SAFE_FILENAME_ENCODING.getLabel());
		if ((v != null) && this.forceSafeFilenames) {
			try {
				this.safeFilenameEncoding = Charset.forName(v.asString());
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Encoding [%s] is not supported", v.asString()), e);
			}
			this.fixFilenames = false;
		} else {
			this.safeFilenameEncoding = null;

			v = this.properties.get(LocalContentStoreSetting.FIX_FILENAMES.getLabel());
			this.fixFilenames = ((v != null) && v.asBoolean());
		}

		v = this.properties.get(LocalContentStoreSetting.FAIL_ON_COLLISIONS.getLabel());
		this.failOnCollisions = ((v != null) && v.asBoolean());

		v = this.properties.get(LocalContentStoreSetting.IGNORE_DESCRIPTOR.getLabel());
		this.ignoreDescriptor = ((v != null) && v.asBoolean());

		v = this.properties.get(LocalContentStoreSetting.USE_WINDOWS_FIX.getLabel());
		this.useWindowsFix = ((v != null) && v.asBoolean());
		// This helps make sure the actual used value is stored
		this.properties.put(LocalContentStoreSetting.USE_WINDOWS_FIX.getLabel(), new CmfValue(this.useWindowsFix));
	}

	protected void initProperties() throws CmfStorageException {
		final boolean forceSafeFilenames = this.settings.getBoolean(LocalContentStoreSetting.FORCE_SAFE_FILENAMES);
		final Charset safeFilenameEncoding;
		final boolean fixFilenames;
		if (forceSafeFilenames) {
			String encoding = this.settings.getString(LocalContentStoreSetting.SAFE_FILENAME_ENCODING);
			try {
				safeFilenameEncoding = Charset.forName(encoding);
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Encoding [%s] is not supported", encoding), e);
			}
			fixFilenames = false;
		} else {
			safeFilenameEncoding = null;
			fixFilenames = this.settings.getBoolean(LocalContentStoreSetting.FIX_FILENAMES);
		}
		final boolean failOnCollisions = this.settings.getBoolean(LocalContentStoreSetting.FAIL_ON_COLLISIONS);
		final boolean ignoreFragment = this.settings.getBoolean(LocalContentStoreSetting.IGNORE_DESCRIPTOR);
		final boolean useWindowsFix = this.settings.getBoolean(LocalContentStoreSetting.USE_WINDOWS_FIX);

		this.properties.put(LocalContentStoreSetting.FORCE_SAFE_FILENAMES.getLabel(), new CmfValue(forceSafeFilenames));
		if (safeFilenameEncoding != null) {
			this.properties.put(LocalContentStoreSetting.SAFE_FILENAME_ENCODING.getLabel(),
				new CmfValue(safeFilenameEncoding.name()));
		}
		this.properties.put(LocalContentStoreSetting.FIX_FILENAMES.getLabel(), new CmfValue(fixFilenames));
		this.properties.put(LocalContentStoreSetting.FAIL_ON_COLLISIONS.getLabel(), new CmfValue(failOnCollisions));
		this.properties.put(LocalContentStoreSetting.IGNORE_DESCRIPTOR.getLabel(), new CmfValue(ignoreFragment));
		this.properties.put(LocalContentStoreSetting.USE_WINDOWS_FIX.getLabel(),
			new CmfValue(useWindowsFix || SystemUtils.IS_OS_WINDOWS));
	}

	@Override
	public File getStoreLocation() {
		return this.baseDir;
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
			str = FilenameEncoder.safeEncode(str, this.useWindowsFix);
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
		CmfContentStream info) {
		final Location location = this.organizer.getLocation(translator, object, info);
		final List<String> rawPath = new ArrayList<>(location.containerSpec);
		rawPath.add(constructFileName(location));

		final String scheme;
		final String ssp;
		if (this.forceSafeFilenames || this.fixFilenames) {
			boolean fixed = false;
			List<String> sspParts = new ArrayList<>();
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
			if (this.failOnCollisions) {
				throw new CmfStorageException(String.format(
					"Filename collision detected for target file [%s] - a file already exists at that location",
					f.getAbsolutePath()));
			}
			if (!f.exists()) {
				throw new CmfStorageException(
					String.format("Failed to create the non-existent target file [%s]", f.getAbsolutePath()));
			}
		}
		try (FileOutputStream out = new FileOutputStream(f)) {
			IOUtils.copyLarge(in, out);
		} catch (FileNotFoundException e) {
			throw new CmfStorageException(String.format("Failed to open the output stream to the file at [%s]", f), e);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to write the content out to the file at [%s]", f), e);
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

	protected synchronized void storeProperties() throws CmfStorageException {
		if (!this.storeProperties) { return; }
		try (OutputStream out = new FileOutputStream(this.propertiesFile)) {
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
			XmlTools.marshal(p, PropertiesLoader.SCHEMA, out, true);
			out.flush();
		} catch (FileNotFoundException e) {
			return;
		} catch (JAXBException e) {
			throw new CmfStorageException("Failed to parse the store properties", e);
		} catch (IOException e) {
			throw new CmfStorageException("Failed to write the store properties", e);
		}
	}

	@Override
	protected LocalStoreOperation newOperation() throws CmfStorageException {
		return new LocalStoreOperation(this.baseDir);
	}

	@Override
	protected void clearAllProperties(LocalStoreOperation operation) throws CmfStorageException {
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
		return new TreeSet<>(this.properties.keySet());
	}

	@Override
	protected CmfValue clearProperty(LocalStoreOperation operation, String property) throws CmfStorageException {
		CmfValue ret = this.properties.remove(property);
		this.modified.set(true);
		return ret;
	}

	@Override
	protected boolean doClose(boolean cleanupIfEmpty) {
		if (this.modified.get() && this.storeProperties) {
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
	protected LocalHandle constructHandle(CmfObject<?> object, CmfContentStream info, URI locator) {
		return new LocalHandle(object, info, locator);
	}
}