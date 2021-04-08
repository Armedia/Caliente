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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;

import com.armedia.caliente.store.CmfAttribute;
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
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
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
public class S3ContentStore extends CmfContentStore<S3Locator, S3StoreOperation> {

	private static final String PROPERTIES_FILE = "caliente-store-properties.xml";
	private static final String MD_ID = "caliente:id";
	private static final String MD_HISTORY_ID = "caliente:historyId";

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
	private final List<String> basePathComponents;
	private final Path tempDir;
	private final boolean storeProperties;
	private final CmfContentOrganizer organizer;
	private final String propertiesFile;
	private final AtomicBoolean modified = new AtomicBoolean(false);
	private final CfgTools settings;
	private final Map<String, CmfValue> properties = new TreeMap<>();
	private final boolean attachMetadata;
	private final boolean supportsVersions;
	private final boolean failOnCollisions;
	protected final boolean propertiesLoaded;

	public S3ContentStore(CmfStore<?> parent, CfgTools settings, boolean cleanData) throws CmfStorageException {
		super(parent);
		if (settings == null) { throw new IllegalArgumentException("Must provide configuration settings"); }
		this.settings = settings;

		this.bucket = settings.getString(S3ContentStoreSetting.BUCKET);
		if (StringUtils.isBlank(this.bucket)) {
			throw new CmfStorageException("Invalid bucket name: [" + this.bucket + "]");
		}

		String localDir = settings.getString("dir.content");
		if (StringUtils.isBlank(localDir)) { throw new CmfStorageException("No setting [dir.content] specified"); }
		this.localDir = Tools.canonicalize(Paths.get(localDir));

		String temp = settings.getString(S3ContentStoreSetting.TEMP);
		if (StringUtils.isNotEmpty(temp)) {
			// Set the default ...
			this.tempDir = Tools.canonicalize(Paths.get(temp));
		} else {
			try {
				FileUtils.forceMkdir(this.localDir.toFile());
				this.tempDir = Files.createTempDirectory(this.localDir, ".s3-temp-");
			} catch (IOException e) {
				throw new CmfStorageException("Failed to create a temporary directory at [" + this.localDir + "]", e);
			}
		}

		this.attachMetadata = settings.getBoolean(S3ContentStoreSetting.ATTACH_METADATA);

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
		boolean supportsVersions = false;
		boolean exists = false;
		try {
			this.client.headBucket((R) -> R.bucket(this.bucket));
			exists = true;
			supportsVersions = (this.client.getBucketVersioning((R) -> R.bucket(this.bucket))
				.status() == BucketVersioningStatus.ENABLED);
		} catch (NoSuchBucketException e) {
			if (!createMissingBucket) {
				throw new CmfStorageException("No bucket named [" + this.bucket + "] was found", e);
			}
		}

		if (!exists) {
			// If the bucket is missing, and we're supposed to create it, then do so
			try {
				this.client.createBucket((R) -> R.bucket(this.bucket));
			} catch (Exception e) {
				throw new CmfStorageException("Failed to create the missing bucket [" + this.bucket + "]", e);
			}

			try {
				this.client.putBucketVersioning((R) -> {
					R.bucket(this.bucket);
					R.versioningConfiguration((C) -> C.status(BucketVersioningStatus.ENABLED));
				});
				supportsVersions = true;
			} catch (Exception e) {
				supportsVersions = false;
				this.log.error("Failed to enable versioning on bucket [{}]", this.bucket, e);
			}
		}
		this.supportsVersions = supportsVersions;

		String basePath = settings.getString(S3ContentStoreSetting.BASE_PATH);
		// TODO: Normalize this path, make sure it has a leading slash
		basePath = FilenameUtils.normalize(basePath, true);
		if (StringUtils.isBlank(basePath)) {
			basePath = StringUtils.EMPTY;
		} else if (!basePath.startsWith("/")) {
			basePath = "/" + basePath;
		}
		this.basePathComponents = FileNameTools.tokenize(basePath.replace('\\', '/'), '/');
		this.basePath = FileNameTools.reconstitute(this.basePathComponents, false, false);

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
	}

	protected void initProperties() throws CmfStorageException {
		final boolean failOnCollisions = this.settings.getBoolean(S3ContentStoreSetting.FAIL_ON_COLLISIONS);
		setProperty(S3ContentStoreSetting.FAIL_ON_COLLISIONS.getLabel(), CmfValue.of(failOnCollisions));
	}

	@Override
	public File getStoreLocation() {
		return null;
	}

	@Override
	protected boolean isSupported(S3Locator locator) {
		return (locator != null);
	}

	protected CharSequence encodeChar(char c) {
		if (S3ContentStore.CHARS_BAD.contains(c)) {
			return "_"; // TODO: Make this configurable?
		}
		/*
		if (S3ContentStore.CHARS_URLENCODE.contains(c)) {
			// URLEncode it
			return String.format("%%%02X", (0xFF & c));
		}
		*/
		return String.valueOf(c);
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
		String ext = loc.extension;

		if (StringUtils.contains("file4.txt", loc.baseName)) {
			"".hashCode();
		}

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
			if (StringUtils.isEmpty(baseName)) {
				baseName = "_CMF_";
			}
		}

		return baseName + ext;
	}

	private <VALUE> Pair<List<String>, String> renderURIParts(CmfAttributeTranslator<VALUE> translator,
		CmfObject<VALUE> object, CmfContentStream info) {
		final Location location = this.organizer.getLocation(translator, object, info);
		final List<String> rawPath = new ArrayList<>(location.containerSpec);
		final String versionId = location.appendix;
		rawPath.add(constructFileName(location));
		List<String> sspParts = new ArrayList<>(rawPath.size());
		for (String s : rawPath) {
			sspParts.add(safeEncode(s));
		}
		return Pair.of(sspParts, versionId);
	}

	@Override
	protected <VALUE> String doRenderContentPath(CmfObject<VALUE> object, CmfContentStream info) {
		return FileNameTools.reconstitute(renderURIParts(object.getTranslator(), object, info).getKey(), false, false,
			'/');
	}

	@Override
	protected <VALUE> S3Locator doCalculateLocator(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		final Pair<List<String>, String> p = renderURIParts(translator, object, info);
		List<String> path = new LinkedList<>(this.basePathComponents);
		path.addAll(p.getKey());

		String fragment = null;
		if (!this.supportsVersions) {
			String fileName = path.remove(path.size() - 1);
			String attName = translator.getAttributeNameMapper().decodeAttributeName(object.getType(),
				"cmis:versionLabel");
			CmfAttribute<VALUE> att = object.getAttribute(attName);
			CmfValue v = translator.encodeAttribute(object.getType(), att).getValue();
			fileName += "." + v.toString();
			path.add(fileName);
			fragment = null;
		}

		S3Locator locator = new S3Locator(this.bucket, FileNameTools.reconstitute(path, false, false, '/'), fragment);
		this.log.debug("Generated the locator {}", locator);
		return locator;
	}

	@Override
	protected final Path getPath(S3Locator locator) {
		return null;
	}

	@Override
	protected ReadableByteChannel openChannel(S3StoreOperation op, S3Locator locator) throws CmfStorageException {
		return Channels.newChannel(this.client.getObject((R) -> {
			locator.bucket(R::bucket);
			locator.key(R::key);
			locator.versionId(R::versionId);
		}));
	}

	@Override
	protected ContentAccessor createTemp(S3Locator locator) throws CmfStorageException {
		try {
			return new ContentAccessor(
				Files.createTempFile(this.tempDir, String.format("%08x", locator.hashCode()), ".tmp"));
		} catch (IOException e) {
			throw new CmfStorageException("Failed to create a temporary file for [" + locator + "]", e);
		}
	}

	@Override
	protected <VALUE> Pair<S3Locator, Long> store(S3StoreOperation op, Handle<VALUE> handle, ReadableByteChannel in,
		long size) throws CmfStorageException {
		// TODO: Do we want to do multipart uploads for large files? If the size exceeds a
		// threshold, we probably should...?

		// TODO: How to handle the generic wildcard?

		final S3Locator locator = getLocator(handle);

		if (this.failOnCollisions) {
			// Check to see if we have a collision - i.e. this object is NOT a different version
			// of the same object
			try {
				HeadObjectResponse rsp = this.client.headObject((R) -> {
					locator.bucket(R::bucket);
					locator.key(R::key);
					locator.versionId(R::versionId);
				});
				if (rsp.hasMetadata()) {
					Map<String, String> metadata = rsp.metadata();
					String expectedHistoryId = metadata.get(S3ContentStore.MD_HISTORY_ID);
					String finalHistoryId = handle.getCmfObject().getHistoryId();
					// If there's no history ID, or it's different than the incoming one,
					// by definition we have a collision
					if (!StringUtils.equals(expectedHistoryId, finalHistoryId)) {
						throw new CmfStorageException("A collision was detected using locator [" + locator + "]");
					}
				}
			} catch (NoSuchKeyException e) {
				// Nothing to do here, no collision!
			}
		}

		// TODO: How do we handle multivalued attributes?
		final Map<String, String> metadata = new LinkedHashMap<>();
		if (this.attachMetadata) {
			final CmfObject<VALUE> cmfObject = handle.getCmfObject();
			final CmfObject.Archetype type = cmfObject.getType();
			final CmfAttributeTranslator<VALUE> translator = handle.getTranslator();
			for (String attributeName : cmfObject.getAttributeNames()) {
				CmfAttribute<VALUE> rawAttribute = cmfObject.getAttribute(attributeName);
				if (!rawAttribute.isMultivalued() || rawAttribute.hasValues()) {
					CmfAttribute<CmfValue> encodedAttribute = translator.encodeAttribute(type, rawAttribute);
					CmfValue v = encodedAttribute.getValue();
					String s = v.asString();

					// Currently, we ignore blank-valued attributes
					if (StringUtils.isNotBlank(s)) {
						metadata.put(rawAttribute.getName(), s);
					}
				}
			}
		}
		metadata.put(S3ContentStore.MD_HISTORY_ID, handle.getCmfObject().getHistoryId());
		metadata.put(S3ContentStore.MD_ID, handle.getCmfObject().getId());

		// Do the upload ...
		PutObjectResponse rsp = this.client.putObject((R) -> {
			locator.bucket(R::bucket);
			locator.key(R::key);
			// TODO: Enable this
			// R.metadata(metadata);
		}, (size < 1 ? RequestBody.empty() : RequestBody.fromInputStream(Channels.newInputStream(in), size)));

		return Pair.of(new S3Locator(locator.bucket(), locator.key(), (this.supportsVersions ? rsp.versionId() : null)),
			size);
	}

	@Override
	protected WritableByteChannel createChannel(final S3StoreOperation op, final S3Locator locator)
		throws CmfStorageException {
		try {
			return new DeferredS3WritableChannel(this.tempDir, this.client, locator);
		} catch (IOException e) {
			throw new CmfStorageException("Failed to open a deferred write channel for [" + locator + "]", e);
		}
	}

	private HeadObjectResponse headObject(S3Locator locator) throws CmfStorageException {
		try {
			return this.client.headObject((R) -> {
				locator.bucket(R::bucket);
				locator.key(R::key);
				locator.versionId(R::versionId);
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
	protected boolean exists(S3StoreOperation op, S3Locator locator) throws CmfStorageException {
		HeadObjectResponse rsp = headObject(locator);
		return (rsp != null) ? (rsp.deleteMarker() != Boolean.TRUE) : false;
	}

	@Override
	protected long getSize(S3StoreOperation op, S3Locator locator) throws CmfStorageException {
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
		for (ListObjectsV2Response list : this.client.listObjectsV2Paginator((R) -> {
			R.bucket(this.bucket);
			R.prefix(this.basePath + "/");
		})) {
			List<S3Object> s3objects = list.contents();
			if (s3objects.isEmpty()) { return; }
			List<ObjectIdentifier> objects = s3objects.stream() //
				.map((o) -> ObjectIdentifier.builder().key(o.key()).build()) //
				.collect(Collectors.toList());
			this.client.deleteObjects((R) -> {
				R.bucket(this.bucket);
				R.delete((D) -> D.objects(objects));
			});
		}
	}

	protected synchronized void storeProperties() throws CmfStorageException {
		if (!this.storeProperties) { return; }
		final S3Locator locator = new S3Locator(this.bucket, this.propertiesFile, null);
		try (WritableByteChannel c = createChannel(newOperation(true), locator)) {
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
	protected String encodeLocator(S3Locator locator) {
		if (locator == null) { return null; }
		return locator.toString();
	}

	@Override
	protected S3Locator decodeLocator(String locator) {
		if (StringUtils.isBlank(locator)) { return null; }
		try {
			return new S3Locator(locator);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to decode the locator [" + locator + "] as a valid S3Locator", e);
		}
	}
}