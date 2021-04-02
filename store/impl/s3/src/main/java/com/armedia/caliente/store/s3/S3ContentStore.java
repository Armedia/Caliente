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

package com.armedia.caliente.store.s3;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;

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
import com.armedia.caliente.store.s3.S3ContentStoreFactory.CredentialType;
import com.armedia.caliente.store.s3.xml.PropertiesLoader;
import com.armedia.caliente.store.s3.xml.PropertyT;
import com.armedia.caliente.store.s3.xml.StorePropertiesT;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.xml.XmlTools;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 *
 *
 */
public class S3ContentStore extends CmfContentStore<URI, S3StoreOperation> {

	private static final String SCHEME = "s3";
	private static final String PROPERTIES_FILE = "caliente-store-properties.xml";

	private static final Set<String> SUPPORTED_SCHEMES = Tools.freezeSet(Collections.singleton(S3ContentStore.SCHEME));

	private static final String CHARS_URLENCODE_STR = "&$@=;:+ ,?";
	private static final Set<Character> CHARS_URLENCODE;
	private static final String CHARS_BAD_STR = "\\{}^%[]`\"~#|<>";
	private static final Set<Character> CHARS_BAD;
	static {
		Set<Character> s = null;

		s = new HashSet<>();
		for (int i = 0; i < S3ContentStore.CHARS_URLENCODE_STR.length(); i++) {
			s.add(S3ContentStore.CHARS_URLENCODE_STR.charAt(i));
		}
		s.add(Character.valueOf((char) 0x7F));
		for (int i = 0x00; i < 0x20; i++) {
			s.add(Character.valueOf((char) i));
		}
		CHARS_URLENCODE = Tools.freezeSet(s);

		s = new HashSet<>();
		for (int i = 0; i < S3ContentStore.CHARS_BAD_STR.length(); i++) {
			s.add(S3ContentStore.CHARS_BAD_STR.charAt(i));
		}
		for (int i = 0x80; i <= 0xFF; i++) {
			s.add(Character.valueOf((char) i));
		}
		CHARS_BAD = Tools.freezeSet(s);
	}

	private final S3Client client;
	private final Path localDir;
	private final String bucket;
	private final String basePath;
	private final Path tempDir;
	private final boolean storeProperties;
	private final CmfContentOrganizer organizer;
	private final String propertiesFile;
	private final AtomicBoolean modified = new AtomicBoolean(false);
	private final CfgTools settings;
	private final Map<String, CmfValue> properties = new TreeMap<>();
	private final boolean failOnCollisions;
	private final boolean ignoreDescriptor;
	protected final boolean propertiesLoaded;
	private final boolean useWindowsFix;

	public S3ContentStore(CmfStore<?> parent, CfgTools settings, boolean cleanData) throws CmfStorageException {
		super(parent);
		if (settings == null) { throw new IllegalArgumentException("Must provide configuration settings"); }
		this.settings = settings;

		String temp = settings.getString(S3ContentStoreSetting.TEMP);
		if (StringUtils.isEmpty(temp)) {
			// Set the default ...
		}

		this.bucket = settings.getString(S3ContentStoreSetting.BUCKET);
		if (StringUtils.isBlank(this.bucket)) {
			throw new CmfStorageException("Invalid bucket name: [" + this.bucket + "]");
		}

		String localDir = settings.getString("dir.content");
		if (StringUtils.isBlank(localDir)) { throw new CmfStorageException("No setting [dir.content] specified"); }
		this.localDir = Tools.canonicalize(Paths.get(localDir));
		try {
			this.tempDir = Files.createTempDirectory(this.localDir, ".s3-temp-");
		} catch (IOException e) {
			throw new CmfStorageException("Failed to create a temporary directory at [" + this.localDir + "]", e);
		}

		final Region region = Region.of(StringUtils.lowerCase(settings.getString(S3ContentStoreSetting.REGION)));
		final String endpoint = settings.getString(S3ContentStoreSetting.ENDPOINT);
		final CredentialType credentialType = settings.getEnum(S3ContentStoreSetting.CREDENTIAL_TYPE,
			CredentialType.class);

		S3ClientBuilder clientBuilder = S3Client.builder();
		clientBuilder.region(region);
		if (StringUtils.isNotBlank(endpoint)) {
			try {
				clientBuilder.endpointOverride(new URI(endpoint));
			} catch (URISyntaxException e) {
				throw new CmfStorageException("Bad endpoint URI [" + endpoint + "]", e);
			}
		}
		clientBuilder.credentialsProvider(credentialType.build(settings));
		this.client = clientBuilder.build();

		final boolean createMissingBucket = settings.getBoolean(S3ContentStoreSetting.CREATE_MISSING_BUCKET);
		if (createMissingBucket) {
			try {
				this.client.headBucket((R) -> {
					R.bucket(this.bucket);
				});
			} catch (NoSuchBucketException e) {
				if (!createMissingBucket) {
					throw new CmfStorageException("No bucket named [" + this.bucket + "] was found", e);
				}
				try {
					this.client.createBucket((R) -> {
						R.bucket(this.bucket);
					});
				} catch (Exception e2) {
					throw new CmfStorageException("Failed to create the missing bucket [" + this.bucket + "]", e2);
				}
			}
			// If the bucket is missing, create it
		}

		String basePath = settings.getString(S3ContentStoreSetting.BASE_PATH);
		// TODO: Normalize this path, make sure it has a leading slash
		basePath = FilenameUtils.normalize(basePath, true);
		if (StringUtils.isBlank(basePath)) {
			basePath = StringUtils.EMPTY;
		}
		this.basePath = basePath;

		CmfContentOrganizer organizer = CmfContentOrganizer
			.getOrganizer(settings.getString(S3ContentStoreSetting.URI_ORGANIZER));
		if (this.log.isDebugEnabled()) {
			this.log.debug("Creating a new local file store with base path [{}], and organizer [{}]", this.basePath,
				organizer.getName());
		}

		this.propertiesFile = this.basePath + "/" + S3ContentStore.PROPERTIES_FILE;
		this.storeProperties = (parent == null) && settings.getBoolean(S3ContentStoreSetting.STORE_PROPERTIES);

		boolean storeOrganizerName = true;
		if (cleanData) {
			this.propertiesLoaded = false;
			clearAllProperties();
			clearAllStreams();
		} else {
			if (parent == null) {
				// First, try the legacy mode...
				boolean propertiesLoaded = false;
				try {
					propertiesLoaded = new PropertiesLoader().loadProperties(this.client, this.bucket,
						this.propertiesFile, this.properties);
				} catch (CmfStorageException e) {
					throw new CmfStorageException("Failed to load both the modern and legacy properties models", e);
				}
				this.propertiesLoaded = propertiesLoaded;
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

		v = getProperty(S3ContentStoreSetting.FAIL_ON_COLLISIONS.getLabel());
		this.failOnCollisions = ((v != null) && v.asBoolean());

		v = getProperty(S3ContentStoreSetting.IGNORE_DESCRIPTOR.getLabel());
		this.ignoreDescriptor = ((v != null) && v.asBoolean());

		v = getProperty(S3ContentStoreSetting.USE_WINDOWS_FIX.getLabel());
		this.useWindowsFix = ((v != null) && v.asBoolean());
		// This helps make sure the actual used value is stored
		setProperty(S3ContentStoreSetting.USE_WINDOWS_FIX.getLabel(), CmfValue.of(this.useWindowsFix));
	}

	protected void initProperties() throws CmfStorageException {
		final boolean forceSafeFilenames = this.settings.getBoolean(S3ContentStoreSetting.FORCE_SAFE_FILENAMES);
		final Charset safeFilenameEncoding;
		final boolean fixFilenames;
		if (forceSafeFilenames) {
			String encoding = this.settings.getString(S3ContentStoreSetting.SAFE_FILENAME_ENCODING);
			try {
				safeFilenameEncoding = Charset.forName(encoding);
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Encoding [%s] is not supported", encoding), e);
			}
			fixFilenames = false;
		} else {
			safeFilenameEncoding = null;
			fixFilenames = this.settings.getBoolean(S3ContentStoreSetting.FIX_FILENAMES);
		}
		final boolean failOnCollisions = this.settings.getBoolean(S3ContentStoreSetting.FAIL_ON_COLLISIONS);
		final boolean ignoreFragment = this.settings.getBoolean(S3ContentStoreSetting.IGNORE_DESCRIPTOR);
		final boolean useWindowsFix = this.settings.getBoolean(S3ContentStoreSetting.USE_WINDOWS_FIX);

		setProperty(S3ContentStoreSetting.FORCE_SAFE_FILENAMES.getLabel(), CmfValue.of(forceSafeFilenames));
		if (safeFilenameEncoding != null) {
			setProperty(S3ContentStoreSetting.SAFE_FILENAME_ENCODING.getLabel(),
				CmfValue.of(safeFilenameEncoding.name()));
		}
		setProperty(S3ContentStoreSetting.FIX_FILENAMES.getLabel(), CmfValue.of(fixFilenames));
		setProperty(S3ContentStoreSetting.FAIL_ON_COLLISIONS.getLabel(), CmfValue.of(failOnCollisions));
		setProperty(S3ContentStoreSetting.IGNORE_DESCRIPTOR.getLabel(), CmfValue.of(ignoreFragment));
		setProperty(S3ContentStoreSetting.USE_WINDOWS_FIX.getLabel(),
			CmfValue.of(useWindowsFix || SystemUtils.IS_OS_WINDOWS));
	}

	@Override
	public File getStoreLocation() {
		return null;
	}

	@Override
	protected boolean isSupported(URI locator) {
		return S3ContentStore.SUPPORTED_SCHEMES.contains(locator.getScheme());
	}

	protected char encodeChar(char c) {
		if (S3ContentStore.CHARS_BAD.contains(c)) {
			return '_'; // TODO: Make this configurable?
		}
		if (S3ContentStore.CHARS_URLENCODE.contains(c)) {
			// URLEncode it
			return 'x';
		}
		return c;
	}

	protected String safeEncode(String str) {
		StringBuilder b = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			b.append(encodeChar(str.charAt(i)));
		}
		return b.toString();
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
				baseName = "_CMF_";
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

	private <VALUE> Pair<String, List<String>> renderURIParts(CmfObject<VALUE> object, CmfContentStream info) {
		final Location location = this.organizer.getLocation(object.getTranslator(), object, info);
		final List<String> rawPath = new ArrayList<>(location.containerSpec);
		rawPath.add(constructFileName(location));
		List<String> sspParts = new ArrayList<>(rawPath.size());
		for (String s : rawPath) {
			sspParts.add(safeEncode(s));
		}
		return Pair.of(S3ContentStore.SCHEME, sspParts);
	}

	@Override
	protected <VALUE> String doRenderContentPath(CmfObject<VALUE> object, CmfContentStream info) {
		return FileNameTools.reconstitute(renderURIParts(object, info).getValue(), false, false, '/');
	}

	@Override
	protected <VALUE> URI doCalculateLocator(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		final Pair<String, List<String>> p = renderURIParts(object, info);
		try {
			URI uri = new URI(p.getLeft(), FileNameTools.reconstitute(p.getRight(), false, false, '/'), null);
			this.log.info("Generated URI {}", uri);
			return uri;
		} catch (URISyntaxException e) {
			throw new RuntimeException(
				String.format("Failed to allocate a handle ID for %s[%s]", object.getType(), object.getId()), e);
		}
	}

	@Override
	protected final Path getPath(URI locator) {
		return null;
	}

	@Override
	protected ReadableByteChannel openChannel(S3StoreOperation op, URI locator) throws CmfStorageException {
		return Channels.newChannel(this.client.getObject((R) -> {
			R.bucket(locator.getHost());
			R.key(locator.getPath());
			R.versionId(locator.getFragment());
		}));
	}

	@Override
	protected ContentAccessor createTemp(URI locator) throws CmfStorageException {
		try {
			return new ContentAccessor(
				Files.createTempFile(this.tempDir, String.format("%08x", locator.hashCode()), ".tmp"));
		} catch (IOException e) {
			throw new CmfStorageException("Failed to create a temporary file for [" + locator + "]", e);
		}
	}

	@Override
	protected Pair<URI, Long> store(S3StoreOperation op, URI locator, ReadableByteChannel in, long size)
		throws CmfStorageException {
		// TODO: Do we want to do multipart uploads for large files? If the size exceeds a
		// threshold, we probably should...?

		// Do the upload ...
		PutObjectResponse rsp = this.client.putObject((R) -> {
			R.bucket(locator.getHost());
			R.key(locator.getPath());
		}, (size < 1 ? RequestBody.empty() : RequestBody.fromInputStream(Channels.newInputStream(in), size)));

		try {
			return Pair.of(new URI(locator.getScheme(), locator.getHost(), locator.getPath(), rsp.versionId()), size);
		} catch (URISyntaxException e) {
			throw new CmfStorageException(
				"Failed to re-render a URI for [" + locator + "] using versionId [" + rsp.versionId() + "]", e);
		}
	}

	@Override
	protected WritableByteChannel createChannel(final S3StoreOperation op, final URI locator)
		throws CmfStorageException {
		try {
			return new DeferredS3WritableChannel(this.tempDir, this.client, locator);
		} catch (IOException e) {
			throw new CmfStorageException("Failed to open a deferred write channel for [" + locator + "]", e);
		}
	}

	private HeadObjectResponse headObject(URI locator) throws CmfStorageException {
		try {
			return this.client.headObject((R) -> {
				R.bucket(locator.getHost());
				R.key(locator.getPath());
				String version = locator.getFragment();
				if (StringUtils.isNotBlank(version)) {
					R.versionId(version);
				}
			});
		} catch (NoSuchKeyException e) {
			return null;
		} catch (S3Exception e) {
			if (e.statusCode() == HttpStatus.SC_BAD_REQUEST) {
				// Bad version ID?
				return null;
			}
			throw new CmfStorageException("Failed to check the status of the object at [" + locator + "]", e);
		}
	}

	@Override
	protected boolean exists(S3StoreOperation op, URI locator) throws CmfStorageException {
		HeadObjectResponse rsp = headObject(locator);
		return (rsp != null) ? (rsp.deleteMarker() != Boolean.TRUE) : false;
	}

	@Override
	protected long getSize(S3StoreOperation op, URI locator) throws CmfStorageException {
		HeadObjectResponse rsp = headObject(locator);
		if ((rsp == null) || (rsp.deleteMarker() == Boolean.TRUE)) {
			throw new CmfStorageException("The object at [" + locator + "] could not be found or is marked as deleted");
		}
		Long length = rsp.contentLength();
		if (length == null) {
			throw new CmfStorageException("Unable to retrieve the content length for the object at [" + locator + "]");
		}
		return length;
	}

	@Override
	public boolean isSupportsFileAccess() {
		return false;
	}

	@Override
	protected File doGetRootLocation() {
		return null;
	}

	@Override
	protected void clearAllStreams(S3StoreOperation op) {
		// TODO: Identify these...
		final String bucket = "";
		final String prefix = "";

		for (ListObjectsV2Response list : this.client.listObjectsV2Paginator((R) -> {
			R.bucket(bucket);
			R.prefix(prefix);
		})) {
			List<S3Object> s3objects = list.contents();
			if (s3objects.isEmpty()) { return; }
			List<ObjectIdentifier> objects = s3objects.stream() //
				.map((o) -> ObjectIdentifier.builder().key(o.key()).build()) //
				.collect(Collectors.toList());
			this.client.deleteObjects((R) -> {
				R.bucket(bucket);
				R.delete((D) -> D.objects(objects));
			});
		}
	}

	protected synchronized void storeProperties() throws CmfStorageException {
		if (!this.storeProperties) { return; }
		final URI uri;
		try {
			uri = new URI(S3ContentStore.SCHEME, this.bucket, this.propertiesFile, null);
		} catch (URISyntaxException e) {
			throw new CmfStorageException("Failed to construct the URI for the properties file at [" + this.bucket
				+ "::" + this.propertiesFile + "]", e);
		}

		try (WritableByteChannel c = createChannel(newOperation(true), uri)) {
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

			XmlTools.marshal(p, PropertiesLoader.SCHEMA, Channels.newOutputStream(c), true);
		} catch (JAXBException e) {
			throw new CmfStorageException("Failed to parse the store properties", e);
		} catch (IOException e) {
			throw new CmfStorageException("Failed to write the store properties", e);
		}
	}

	@Override
	protected S3StoreOperation newOperation(boolean exclusive) throws CmfStorageException {
		return new S3StoreOperation(this.client, exclusive);
	}

	@Override
	protected void clearAllProperties(S3StoreOperation operation) throws CmfStorageException {
		this.modified.set(true);
		this.properties.clear();
		this.client.deleteObject((R) -> {
			R.bucket(this.bucket);
			R.key(this.propertiesFile);
		});
		this.modified.set(false);
	}

	@Override
	protected CmfValue getProperty(S3StoreOperation operation, String property) throws CmfStorageException {
		return this.properties.get(property);
	}

	@Override
	protected CmfValue setProperty(S3StoreOperation operation, String property, CmfValue value)
		throws CmfStorageException {
		CmfValue ret = this.properties.put(property, value);
		this.modified.set(true);
		return ret;
	}

	@Override
	protected Set<String> getPropertyNames(S3StoreOperation operation) throws CmfStorageException {
		return new TreeSet<>(this.properties.keySet());
	}

	@Override
	protected CmfValue clearProperty(S3StoreOperation operation, String property) throws CmfStorageException {
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
				this.log.error("Failed to write the store properties to [{}::{}]", this.bucket, this.propertiesFile, e);
			}
		}
		if (Files.exists(this.tempDir)) {
			FileUtils.deleteQuietly(this.tempDir.toFile());
		}
		try {
			this.client.close();
		} catch (Exception e) {
			this.log.error("Failed to close the S3 Client instance", e);
		}
		return super.doClose(cleanupIfEmpty);
	}

	@Override
	protected void clearAllProperties(S3StoreOperation operation, String prefix) throws CmfStorageException {
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
	protected Set<String> getPropertyNames(S3StoreOperation operation, String prefix) throws CmfStorageException {
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
	protected String encodeLocator(URI locator) {
		if (locator == null) { return null; }
		return locator.toString();
	}

	@Override
	protected URI decodeLocator(String locator) {
		if (locator == null) { return null; }
		try {
			return new URI(locator);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(String.format("Failed to construct a URI from [%s]", locator), e);
		}
	}
}