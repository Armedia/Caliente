/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.store.local;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentOrganizer;
import com.armedia.caliente.store.CmfContentOrganizer.Location;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.caliente.store.local.xml.PropertiesLoader;
import com.armedia.caliente.store.local.xml.PropertyT;
import com.armedia.caliente.store.local.xml.StorePropertiesT;
import com.armedia.caliente.store.tools.FilenameEncoder;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.xml.XmlTools;

/**
 *
 *
 */
public class LocalContentStore extends CmfContentStore<LocalContentLocator, LocalContentStoreOperation> {

	private final File baseDir;
	private final File tempDir;
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

	public LocalContentStore(CmfStore<?> parent, CfgTools settings, File baseDir, CmfContentOrganizer organizer,
		boolean cleanData) throws CmfStorageException {
		super(parent);
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
		File f = Tools.canonicalize(baseDir);
		this.baseDir = f;
		try {
			this.tempDir = Files.createTempDirectory(baseDir.toPath(), ".temp").toFile();
		} catch (IOException e) {
			throw new CmfStorageException("Failed to create the temporary data directory at [" + baseDir + "]", e);
		}
		this.storeProperties = (parent == null) && settings.getBoolean(LocalContentStoreSetting.STORE_PROPERTIES);

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
			clearAllProperties();
			clearAllStreams();
		} else {
			if (parent == null) {
				// First, try the legacy mode...
				try {
					this.propertiesLoaded = new PropertiesLoader().loadProperties(this.propertiesFile, this.properties);
				} catch (CmfStorageException e) {
					throw new CmfStorageException("Failed to load both the properties models", e);
				}
			} else {
				this.propertiesLoaded = true;
			}

			CmfValue currentOrganizerName = getProperty("organizer");
			if ((currentOrganizerName == null) || currentOrganizerName.isNull()) {
				// For backwards compatibility
				currentOrganizerName = getProperty("strategy");
			}
			if ((currentOrganizerName != null) && !currentOrganizerName.isNull()) {
				CmfContentOrganizer savedOrganizer = CmfContentOrganizer.getOrganizer(currentOrganizerName.asString());
				if (savedOrganizer != null) {
					organizer = savedOrganizer;
					storeOrganizerName = false;
				}
			}
		}
		this.organizer = organizer;
		if (this.organizer == null) { throw new IllegalArgumentException("Must provide a content organizer"); }

		this.organizer.configure(settings);
		if (storeOrganizerName) {
			setProperty("organizer", CmfValue.of(organizer.getName()));
		}

		// This seems clunky but it's actually very useful - it allows us to load properties
		// and apply them at constructor time in a consistent fashion...
		if (!this.propertiesLoaded) {
			initProperties();
		}

		CmfValue v = null;
		v = getProperty(LocalContentStoreSetting.FORCE_SAFE_FILENAMES.getLabel());
		this.forceSafeFilenames = ((v != null) && v.asBoolean());

		v = getProperty(LocalContentStoreSetting.SAFE_FILENAME_ENCODING.getLabel());
		if ((v != null) && this.forceSafeFilenames) {
			try {
				this.safeFilenameEncoding = Charset.forName(v.asString());
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Encoding [%s] is not supported", v.asString()), e);
			}
			this.fixFilenames = false;
		} else {
			this.safeFilenameEncoding = null;

			v = getProperty(LocalContentStoreSetting.FIX_FILENAMES.getLabel());
			this.fixFilenames = ((v != null) && v.asBoolean());
		}

		v = getProperty(LocalContentStoreSetting.FAIL_ON_COLLISIONS.getLabel());
		this.failOnCollisions = ((v != null) && v.asBoolean());

		v = getProperty(LocalContentStoreSetting.IGNORE_DESCRIPTOR.getLabel());
		this.ignoreDescriptor = ((v != null) && v.asBoolean());

		v = getProperty(LocalContentStoreSetting.USE_WINDOWS_FIX.getLabel());
		this.useWindowsFix = ((v != null) && v.asBoolean());
		// This helps make sure the actual used value is stored
		setProperty(LocalContentStoreSetting.USE_WINDOWS_FIX.getLabel(), CmfValue.of(this.useWindowsFix));
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

		setProperty(LocalContentStoreSetting.FORCE_SAFE_FILENAMES.getLabel(), CmfValue.of(forceSafeFilenames));
		if (safeFilenameEncoding != null) {
			setProperty(LocalContentStoreSetting.SAFE_FILENAME_ENCODING.getLabel(),
				CmfValue.of(safeFilenameEncoding.name()));
		}
		setProperty(LocalContentStoreSetting.FIX_FILENAMES.getLabel(), CmfValue.of(fixFilenames));
		setProperty(LocalContentStoreSetting.FAIL_ON_COLLISIONS.getLabel(), CmfValue.of(failOnCollisions));
		setProperty(LocalContentStoreSetting.IGNORE_DESCRIPTOR.getLabel(), CmfValue.of(ignoreFragment));
		setProperty(LocalContentStoreSetting.USE_WINDOWS_FIX.getLabel(),
			CmfValue.of(useWindowsFix || SystemUtils.IS_OS_WINDOWS));
	}

	@Override
	public File getStoreLocation() {
		return this.baseDir;
	}

	@Override
	protected boolean isSupported(LocalContentLocator locator) {
		return LocalContentLocator.isSupported(locator.getScheme());
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

	private String constructFileName(Location loc) {
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

	private <VALUE> Pair<String, List<String>> renderSafePartsIfNecessary(CmfObject<VALUE> object, CmfContentStream info) {
		final Location location = this.organizer.getLocation(object.getTranslator(), object, info);
		final List<String> rawPath = new ArrayList<>(location.containerSpec);
		rawPath.add(constructFileName(location));

		if (!this.forceSafeFilenames && !this.fixFilenames) { return Pair.of(LocalContentLocator.SCHEME_RAW, rawPath); }

		final String scheme;
		boolean fixed = false;
		List<String> sspParts = new ArrayList<>();
		for (String s : rawPath) {
			String S = safeEncode(s);
			fixed |= !Objects.equals(s, S);
			sspParts.add(S);
		}

		if (fixed) {
			if (this.forceSafeFilenames) {
				scheme = LocalContentLocator.SCHEME_SAFE;
			} else {
				scheme = LocalContentLocator.SCHEME_FIXED;
			}
		} else {
			scheme = LocalContentLocator.SCHEME_RAW;
		}

		return Pair.of(scheme, sspParts);
	}

	@Override
	public <VALUE> String renderContentPath(CmfObject<VALUE> object, CmfContentStream info) {
		return FileNameTools.reconstitute(renderSafePartsIfNecessary(object, info).getValue(), false, false, '/');
	}

	protected String encodePath(String p) {
		return p;
	}

	protected String decodePath(String p) {
		return p;
	}

	@Override
	protected <VALUE> LocalContentLocator doCalculateLocator(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		final Pair<String, List<String>> p = renderSafePartsIfNecessary(object, info);
		LocalContentLocator locator = new LocalContentLocator(p.getLeft(),
			FileNameTools.reconstitute(p.getRight(), false, false, '/'));
		this.log.info("Generated Locator {}", locator);
		return locator;
	}

	@Override
	protected final Path getPath(LocalContentLocator locator) {
		return new File(this.baseDir, locator.getPath()).toPath();
	}

	@Override
	protected FileChannel openChannel(LocalContentStoreOperation op, LocalContentLocator locator) throws CmfStorageException {
		final Path p = getPath(locator);
		try {
			return FileChannel.open(p, StandardOpenOption.READ);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to open the file at [%s] for input", p), e);
		}
	}

	@Override
	protected ContentAccessor createTemp(LocalContentLocator locator) throws CmfStorageException {
		try {
			FileUtils.forceMkdir(this.tempDir);
			return new ContentAccessor(
				Files.createTempFile(this.tempDir.toPath(), String.format("%08x", locator.hashCode()), ".tmp"));
		} catch (IOException e) {
			throw new CmfStorageException("Failed to create a temporary file for [" + locator + "]", e);
		}
	}

	@Override
	protected <VALUE> Pair<LocalContentLocator, Long> store(LocalContentStoreOperation op,
		CmfContentStore<LocalContentLocator, LocalContentStoreOperation>.Handle<VALUE> handle, ReadableByteChannel in, long size)
		throws CmfStorageException {
		LocalContentLocator locator = getLocator(handle);
		try (FileChannel channel = createChannel(op, locator)) {
			return Pair.of(null, channel.transferFrom(channel, 0, Long.MAX_VALUE));
		} catch (IOException e) {
			throw new CmfStorageException(
				String.format("Failed to transfer the contents to the stream at locator [%s]", locator), e);
		}
	}

	@Override
	protected FileChannel createChannel(LocalContentStoreOperation op, LocalContentLocator locator) throws CmfStorageException {
		final Path p = getPath(locator);

		Path parent = p.getParent();
		if ((parent != null) && !Files.exists(parent)) {
			try {
				Files.createDirectories(parent);
			} catch (IOException e) {
				throw new CmfStorageException(
					String.format("Failed to create the parent path [%s] for [%s]", parent, p), e);
			}
		}

		boolean created = false;
		try {
			Files.createFile(p);
			created = true;
		} catch (FileAlreadyExistsException e) {
			created = false;
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to create the file at [%s]", p), e);
		}
		if (!created) {
			if (this.failOnCollisions) {
				throw new CmfStorageException(String.format(
					"Filename collision detected for target file [%s] - a file already exists at that location", p));
			}
			if (!Files.exists(p)) {
				throw new CmfStorageException(String.format("Failed to create the non-existent target file [%s]", p));
			}
		}
		try {
			return FileChannel.open(p, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to open the FileChannel to the file at [%s]", p), e);
		}
	}

	@Override
	protected boolean exists(LocalContentStoreOperation op, LocalContentLocator locator) throws CmfStorageException {
		final Path p = getPath(locator);
		return Files.exists(p);
	}

	@Override
	protected long getSize(LocalContentStoreOperation op, LocalContentLocator locator) throws CmfStorageException {
		final Path p = getPath(locator);
		try {
			return (Files.exists(p) ? Files.size(p) : -1);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to read the size of the file at [%s]", p), e);
		}
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
	protected void clearAllStreams(LocalContentStoreOperation op) {
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
					this.log.warn("Failed to serialize the value for store property [{}]:  [{}]", n, v);
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
	protected LocalContentStoreOperation newOperation(boolean exclusive) throws CmfStorageException {
		return new LocalContentStoreOperation(this.baseDir, exclusive);
	}

	@Override
	protected void clearAllProperties(LocalContentStoreOperation operation) throws CmfStorageException {
		this.modified.set(true);
		this.properties.clear();
		this.propertiesFile.delete();
		this.modified.set(false);
	}

	@Override
	protected CmfValue getProperty(LocalContentStoreOperation operation, String property) throws CmfStorageException {
		return this.properties.get(property);
	}

	@Override
	protected CmfValue setProperty(LocalContentStoreOperation operation, String property, CmfValue value)
		throws CmfStorageException {
		CmfValue ret = this.properties.put(property, value);
		this.modified.set(true);
		return ret;
	}

	@Override
	protected Set<String> getPropertyNames(LocalContentStoreOperation operation) throws CmfStorageException {
		return new TreeSet<>(this.properties.keySet());
	}

	@Override
	protected CmfValue clearProperty(LocalContentStoreOperation operation, String property) throws CmfStorageException {
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
				this.log.error("Failed to write the store properties to [{}]", this.propertiesFile.getAbsolutePath(),
					e);
			}
		}
		if (this.tempDir.exists()) {
			FileUtils.deleteQuietly(this.tempDir);
		}
		return super.doClose(cleanupIfEmpty);
	}

	@Override
	protected void clearAllProperties(LocalContentStoreOperation operation, String prefix) throws CmfStorageException {
		prefix = String.format("%s.", prefix);
		Set<String> deletions = new LinkedHashSet<>();
		for (String s : this.properties.keySet()) {
			if (s.startsWith(prefix)) {
				deletions.add(s);
			}
		}
		this.properties.keySet().removeAll(deletions);
	}

	@Override
	protected Set<String> getPropertyNames(LocalContentStoreOperation operation, String prefix) throws CmfStorageException {
		prefix = String.format("%s.", prefix);
		Set<String> matches = new TreeSet<>();
		for (String s : this.properties.keySet()) {
			if (s.startsWith(prefix)) {
				matches.add(s.substring(prefix.length()));
			}
		}
		return matches;
	}

	@Override
	protected String encodeLocator(LocalContentLocator locator) {
		if (locator == null) { return null; }
		return locator.encode();
	}

	@Override
	protected LocalContentLocator decodeLocator(String locator) {
		if (locator == null) { return null; }
		try {
			return LocalContentLocator.decode(locator);
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Failed to construct a locator from [%s]", locator), e);
		}
	}
}