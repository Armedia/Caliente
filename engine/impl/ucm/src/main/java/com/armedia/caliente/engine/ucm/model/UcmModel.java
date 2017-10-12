package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSession.RequestPreparation;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;
import oracle.stellent.ridc.protocol.ServiceResponse.ResponseType;

// FLD_CREATE_FILE (fParentGUID, dDocName, fRelationshipType="(owner|soft)")
// FLD_CREATE_FILE (fParentGUID, fFileGUID, fRelationshipType="(owner|soft)") (???)
// FLD_CREATE_FOLDER (fParentGUID, fFolderGUID, fRelationshipType="(owner|soft)")
public class UcmModel {
	private static final Pattern PATH_CHECKER = Pattern.compile("^(/|(/[^/]+)+/?)$");
	private static final String PRIMARY = "primary";
	private static final int MIN_OBJECT_COUNT = 100;
	private static final int DEFAULT_OBJECT_COUNT = 10000;
	private static final int MAX_OBJECT_COUNT = 1000000;

	private static final String FILE_SCHEME = "file";
	private static final String FOLDER_SCHEME = "folder";
	private static final String NULL_SCHEME = "null";

	private static final URI NULL_URI = UcmModel.newURI(UcmModel.NULL_SCHEME, "null");
	private static final URI NULL_FOLDER_URI = UcmModel.newFolderURI("idcnull");

	static final URI ROOT_URI = UcmModel.newFolderURI("FLD_ROOT");

	// Unique URI:
	// * FILE -> file:${fFileGUID}#${dID}
	// * FOLDER -> folder:${fFolderGUID}

	// Unique URI -> DataObject
	private final KeyLockableCache<UcmUniqueURI, UcmAttributes> objectByUniqueURI;

	// path -> URI
	private final KeyLockableCache<String, URI> uriByPaths;

	// GUID -> URI
	private final KeyLockableCache<String, URI> uriByGUID;

	// child URI -> parent URI
	private final KeyLockableCache<URI, URI> parentByURI;

	// parent URI -> Map<childName, child URI>
	private final KeyLockableCache<URI, Map<String, URI>> childrenByURI;

	// History URI -> List<UcmUniqueURI>
	private final KeyLockableCache<URI, List<UcmRevision>> historyByURI;

	// String -> Unique URI
	private final KeyLockableCache<String, UcmUniqueURI> revisionUriByRevisionID;

	// Unique URI -> Map<String, UcmRenditionInfo>
	private final KeyLockableCache<UcmUniqueURI, Map<String, UcmRenditionInfo>> renditionsByUniqueURI;

	// History URI -> Unique URI
	private final KeyLockableCache<URI, UcmUniqueURI> uniqueUriByHistoryUri;

	// UniqueURI -> History URI
	private final KeyLockableCache<UcmUniqueURI, URI> historyUriByUniqueURI;

	public static boolean isFrameworkFoldersEnabled(UcmSession s) throws UcmServiceException {
		try {
			ServiceResponse response = null;
			DataBinder responseData = null;
			try {
				response = s.callService("CONFIG_INFO");
				responseData = response.getResponseAsBinder();
			} catch (final IdcClientException e) {
				throw new UcmServiceException(
					"Failed to retrieve the system configuration information using CONFIG_INFO", e);
			}

			// First things first!! Stash the retrieved object...
			DataResultSet rs = responseData.getResultSet("EnabledComponents");
			List<DataObject> components = rs.getRows();
			for (DataObject component : components) {
				if (("FrameworkFolders".equals(component.get("name")))
					&& ("Enabled".equals(component.get("status")))) { return true; }
			}
			return false;
		} catch (Exception e) {
			throw new UcmServiceException(e);
		}
	}

	public UcmModel() throws UcmServiceException {
		this(UcmModel.DEFAULT_OBJECT_COUNT);
	}

	public UcmModel(int objectCount) throws UcmServiceException {
		objectCount = Tools.ensureBetween(UcmModel.MIN_OBJECT_COUNT, objectCount, UcmModel.MAX_OBJECT_COUNT);
		this.uriByPaths = new KeyLockableCache<>(objectCount);
		this.uriByGUID = new KeyLockableCache<>(objectCount);
		this.parentByURI = new KeyLockableCache<>(objectCount);
		this.childrenByURI = new KeyLockableCache<>(objectCount);
		this.historyByURI = new KeyLockableCache<>(objectCount);
		this.objectByUniqueURI = new KeyLockableCache<>(objectCount);
		this.uniqueUriByHistoryUri = new KeyLockableCache<>(objectCount);
		this.historyUriByUniqueURI = new KeyLockableCache<>(objectCount);
		this.renditionsByUniqueURI = new KeyLockableCache<>(objectCount);
		this.revisionUriByRevisionID = new KeyLockableCache<>(objectCount);
	}

	protected void cacheDataObject(final UcmAttributes data) {
		if (data == null) { return; }
		// Is this a file or a folder?
		final URI uri = UcmModel.getURI(data);
		final UcmUniqueURI guid = UcmModel.getUniqueURI(data);

		this.objectByUniqueURI.put(guid, data);

		if (data.hasAttribute(UcmAtt.fParentGUID)) {
			this.parentByURI.put(uri, UcmModel.newFolderURI(data.getString(UcmAtt.fParentGUID)));
		}

		this.uniqueUriByHistoryUri.put(uri, guid);
		this.historyUriByUniqueURI.put(guid, uri);
	}

	protected <K, V> V createIfAbsentInCache(KeyLockableCache<K, V> cache, K key,
		ConcurrentInitializer<V> initializer) {
		try {
			return cache.createIfAbsent(key, initializer);
		} catch (Exception e) {
			// Never gonna happen...
			throw new UcmRuntimeException("Unexpected Exception", e);
		}
	}

	protected static final URI getURI(UcmAttributes data) {
		final boolean file = data.hasAttribute(UcmAtt.fFileGUID) || data.hasAttribute(UcmAtt.dDocName);
		return (file ? UcmModel.newFileURI(data.getString(UcmAtt.fFileGUID))
			: UcmModel.newFolderURI(data.getString(UcmAtt.fFolderGUID)));
	}

	protected static final UcmUniqueURI getUniqueURI(UcmAttributes data) {
		URI uri = UcmModel.getURI(data);
		if (UcmModel.isFileURI(uri)) {
			final String dID = data.getString(UcmAtt.dID);
			try {
				uri = new URI(uri.getScheme(), uri.getSchemeSpecificPart(), dID);
			} catch (URISyntaxException e) {
				throw new UcmRuntimeException(
					String.format("Failed to construct a file URI from [%s] and dID=[%s]", uri, dID), e);
			}
		}
		return new UcmUniqueURI(uri);
	}

	private static final URI newURI(String scheme, String ssp) {
		return UcmModel.newURI(scheme, ssp, null);
	}

	protected static final URI newFileURI(String ssp) {
		return UcmModel.newURI(UcmModel.FILE_SCHEME, ssp, null);
	}

	protected static final URI newFolderURI(String ssp) {
		return UcmModel.newURI(UcmModel.FOLDER_SCHEME, ssp, null);
	}

	protected static final URI newURI(String scheme, String ssp, String fragment) {
		if (StringUtils.isEmpty(scheme)) { throw new IllegalArgumentException("The URI scheme may not be empty"); }
		if (StringUtils
			.isEmpty(ssp)) { throw new IllegalArgumentException("The URI scheme-specific part may not be empty"); }
		try {
			return new URI(scheme, ssp, fragment);
		} catch (URISyntaxException e) {
			throw new RuntimeException(
				String.format("Failed to construct a URI using ([%s], [%s], [%s])", scheme, ssp, fragment), e);
		}
	}

	protected final boolean isNotFoundException(Throwable e, String fmt, Object... args) throws UcmServiceException {
		if (e == null) { return false; }

		// Is this a service exception from which we can identify that the
		// item doesn't exist?
		if (!ServiceException.class.isInstance(e)) {
			// No, this isn't an exception we can analyze...
			throw new UcmServiceException(String.format(fmt, args), e);
		}

		// This may be an analyzable exception
		ServiceException se = ServiceException.class.cast(e);
		DataBinder binder = se.getBinder();
		DataObject local = binder.getLocalData();

		int statusCode = local.getInteger("StatusCode");
		if (statusCode == -16) { return true; }

		String mk = local.get("StatusMessageKey");
		for (UcmExceptionData.Entry entry : UcmExceptionData.parseMessageKey(mk)) {
			String op = entry.getTag();
			if (Tools.equals(op, "csFldDoesNotExist") || //
				Tools.equals(op, "csUnableToGetRevInfo2") || //
				Tools.equals(op, "csGetFileUnableToFindRevision")) {
				// TODO: Maybe we have to index more error labels here?
				return true;
			}
		}
		return false;
	}

	public static final boolean isFileURI(URI uri) {
		Objects.requireNonNull(uri, "Must provide a non-null URI to check");
		return UcmModel.FILE_SCHEME.equals(uri.getScheme());
	}

	public static final boolean isFolderURI(URI uri) {
		Objects.requireNonNull(uri, "Must provide a non-null URI to check");
		return UcmModel.FOLDER_SCHEME.equals(uri.getScheme());
	}

	public static final boolean isShortcut(UcmAttributes att) {
		Objects.requireNonNull(att, "Must provide a non-null attribute set to check");
		return !StringUtils.isEmpty(att.getString(UcmAtt.fTargetGUID));
	}

	public UcmFSObject getObject(UcmSession s, String path) throws UcmServiceException, UcmObjectNotFoundException {
		final URI uri = resolvePath(s, path);
		return newFSObject(uri, getDataObject(s, uri));
	}

	public UcmFile getFile(UcmSession s, String path) throws UcmServiceException, UcmFileNotFoundException {
		URI uri = null;
		try {
			uri = resolvePath(s, path);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}

		return getFile(s, uri);
	}

	public UcmFile getFile(UcmSession s, URI uri) throws UcmServiceException, UcmFileNotFoundException {
		// Ensure the target is a file...
		if (!UcmModel.isFileURI(
			uri)) { throw new UcmFileNotFoundException(String.format("The object with URI [%s] is not a file", uri)); }

		try {
			return new UcmFile(this, uri, getDataObject(s, uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFile getFileByGUID(UcmSession s, String guid) throws UcmServiceException, UcmFileNotFoundException {
		try {
			URI uri = resolveGuid(s, guid, UcmObjectType.FILE);
			return new UcmFile(this, uri, getDataObject(s, uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFolder getFolderByGUID(UcmSession s, String guid) throws UcmServiceException, UcmFolderNotFoundException {
		try {
			URI uri = resolveGuid(s, guid, UcmObjectType.FOLDER);
			return new UcmFolder(this, uri, getDataObject(s, uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	protected UcmFSObject newFSObject(URI uri, UcmAttributes att) {
		return UcmModel.isFileURI(uri) ? new UcmFile(this, uri, att) : new UcmFolder(this, uri, att);
	}

	protected URI resolvePath(final UcmSession s, String p) throws UcmServiceException, UcmObjectNotFoundException {
		final String sanitizedPath = UcmModel.sanitizePath(p);
		URI uri = this.uriByPaths.get(sanitizedPath);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		if (uri == null) {
			try {
				uri = this.uriByPaths.createIfAbsent(sanitizedPath, new ConcurrentInitializer<URI>() {
					@Override
					public URI get() throws ConcurrentException {
						try {
							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.callService("FLD_INFO", new RequestPreparation() {
									@Override
									public void prepareRequest(DataBinder binder) {
										binder.putLocal("path", sanitizedPath);
									}
								});
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the file at [%s]",
									sanitizedPath)) { return UcmModel.NULL_URI; }
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							// First things first!! Stash the retrieved object...
							boolean file = true;
							DataResultSet rs = responseData.getResultSet("FileInfo");
							if (rs == null) {
								file = false;
								rs = responseData.getResultSet("FolderInfo");
							}
							if (rs == null) { throw new UcmServiceException(String.format(
								"Path [%s] was found, but was neither a file nor a folder?!?", sanitizedPath)); }
							Map<String, String> baseObj = new HashMap<>();
							baseObj.putAll(rs.getRows().get(0));
							// Capture the parent path - it's either LocalData.filePath or
							// LocalData.folderPath...but it also contains the filename so we need
							// to dirname it
							String parentPath = responseData
								.getLocal(String.format("%sPath", file ? "file" : "folder"));
							baseObj.put(UcmAtt.cmfParentPath.name(), FileNameTools.dirname(parentPath, '/'));
							data.set(new UcmAttributes(baseObj, rs.getFields()));

							String guid = data.get().getString(UcmAtt.fFileGUID);
							if (guid != null) { return UcmModel.newFileURI(guid); }

							guid = data.get().getString(UcmAtt.fFolderGUID);
							if (guid != null) { return UcmModel.newFolderURI(guid); }

							throw new UcmServiceException(String.format(
								"Path [%s] was found, but was neither a file nor a folder (no identifier attributes)?!?",
								sanitizedPath));
						} catch (IdcClientException | UcmException e) {
							throw new ConcurrentException(e);
						}
					}
				});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				throw new UcmServiceException(String.format("Exception caught searching for path [%s]", sanitizedPath),
					cause);
			}
		}

		// There's an object...so stash it
		cacheDataObject(data.get());

		if (Tools.equals(UcmModel.NULL_URI, uri)) { throw new UcmObjectNotFoundException(
			String.format("No object found at path [%s]", sanitizedPath)); }
		return uri;
	}

	protected URI resolveGuid(final UcmSession s, final String guid, final UcmObjectType type)
		throws UcmServiceException, UcmObjectNotFoundException {
		URI uri = this.uriByGUID.get(guid);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		if (uri == null) {
			try {
				uri = this.uriByPaths.createIfAbsent(guid, new ConcurrentInitializer<URI>() {
					@Override
					public URI get() throws ConcurrentException {
						try {
							ServiceResponse response = null;
							DataBinder responseData = null;
							final UcmAtt uriIdentifierAtt;
							switch (type) {
								case FILE:
									uriIdentifierAtt = UcmAtt.fFileGUID;
									break;
								case FOLDER:
									uriIdentifierAtt = UcmAtt.fFolderGUID;
									break;

								default:
									throw new UcmServiceException(
										String.format("Unsupported object type %s", type.name()));
							}
							final String resultSet = String.format("%sInfo",
								StringUtils.capitalize(type.name().toLowerCase()));

							try {
								response = s.callService("FLD_INFO", new RequestPreparation() {
									@Override
									public void prepareRequest(DataBinder binder) {
										binder.putLocal(uriIdentifierAtt.name(), guid);
									}
								});
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the %s with GUID [%s]",
									type.name(), guid)) { return UcmModel.NULL_URI; }
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							// First things first!! Stash the retrieved object...
							DataResultSet rs = responseData.getResultSet(resultSet);
							if (rs == null) {
								"".hashCode();
								throw new UcmServiceException(String
									.format("%s GUID [%s] was found, didn't contain any data?!?", type.name(), guid));
							}
							Map<String, String> baseObj = new HashMap<>();
							baseObj.putAll(rs.getRows().get(0));
							// Capture the parent path - it's either LocalData.filePath or
							// LocalData.folderPath...but it also contains the filename so we need
							// to dirname it
							String parentPath = responseData
								.getLocal(String.format("%sPath", type.name().toLowerCase()));
							baseObj.put(UcmAtt.cmfParentPath.name(), FileNameTools.dirname(parentPath, '/'));
							data.set(new UcmAttributes(baseObj, rs.getFields()));

							String uriIdentifier = data.get().getString(uriIdentifierAtt);
							if (uriIdentifier != null) { return UcmModel.getURI(data.get()); }

							throw new UcmServiceException(
								String.format("%s GUID [%s] was found, returned no results (no value for %s)?!?",
									type.name(), guid, uriIdentifierAtt.name()));
						} catch (IdcClientException | UcmException e) {
							throw new ConcurrentException(e);
						}
					}
				});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				throw new UcmServiceException(
					String.format("Exception caught searching for %s GUID [%s]", type.name(), guid), cause);
			}
		}

		// There's an object...so stash it
		cacheDataObject(data.get());

		if (Tools.equals(UcmModel.NULL_URI, uri)) { throw new UcmObjectNotFoundException(
			String.format("No %s found with GUID [%s]", type.name(), guid)); }
		return uri;
	}

	protected UcmAttributes getDataObject(final UcmSession s, final URI uri)
		throws UcmServiceException, UcmObjectNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to retrieve");
		if (UcmModel.NULL_FOLDER_URI.equals(uri)) {
			// Take a quick shortcut to avoid unnecessary calls
			throw new UcmObjectNotFoundException(
				String.format("The folder [%s] is not a valid folder URI - it's a null pointer", uri));
		}

		final boolean file;
		if (UcmModel.isFileURI(uri)) {
			// The SSP is the fFileGUID
			file = true;
		} else if (UcmModel.isFolderURI(uri)) {
			// The SSP is the fFolderGUID
			file = false;
		} else {
			// WTF?? Invalid URI
			throw new IllegalArgumentException(String.format("The URI [%s] doesn't point to a valid object", uri));
		}

		UcmUniqueURI guid = this.uniqueUriByHistoryUri.get(uri);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		if (guid == null) {
			try {
				guid = this.uniqueUriByHistoryUri.createIfAbsent(uri, new ConcurrentInitializer<UcmUniqueURI>() {
					@Override
					public UcmUniqueURI get() throws ConcurrentException {
						try {
							final String prefix = (file ? "File" : "Folder");
							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.callService("FLD_INFO", new RequestPreparation() {
									@Override
									public void prepareRequest(DataBinder binder) {
										binder.putLocal(String.format("f%sGUID", prefix), uri.getSchemeSpecificPart());
									}
								});
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the URI [%s]",
									uri)) { return UcmUniqueURI.NULL_GUID; }
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							// First things first!! Stash the retrieved object...
							DataResultSet rs = responseData.getResultSet(String.format("%sInfo", prefix));
							if (rs == null) { throw new UcmServiceException(
								String.format("URI [%s] was found, but returned incorrect results?!?", uri)); }

							Map<String, String> baseObj = new HashMap<>();
							baseObj.putAll(rs.getRows().get(0));
							String parentPath = responseData
								.getLocal(String.format("%sPath", file ? "file" : "folder"));
							baseObj.put(UcmAtt.cmfParentPath.name(), FileNameTools.dirname(parentPath, '/'));

							UcmAttributes baseData = new UcmAttributes(baseObj, rs.getFields());
							data.set(baseData);
							return UcmModel.getUniqueURI(baseData);
						} catch (IdcClientException | UcmException e) {
							throw new ConcurrentException(e);
						}
					}
				});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				throw new UcmServiceException(String.format("Exception caught resolving URI [%s]", uri), cause);
			}
		}

		if (UcmUniqueURI.NULL_GUID.equals(
			guid)) { throw new UcmObjectNotFoundException(String.format("No object found with URI [%s]", uri)); }

		UcmAttributes ret = createIfAbsentInCache(this.objectByUniqueURI, guid,
			new ConcurrentInitializer<UcmAttributes>() {
				@Override
				public UcmAttributes get() throws ConcurrentException {
					UcmAttributes ret = data.get();
					cacheDataObject(ret);
					return ret;
				}
			});

		return ret;
	}

	public UcmFile getFileRevision(UcmSession s, UcmRevision revision)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return new UcmFile(this, revision.getUri(), getFileRevision(s, revision.getId(), true));
	}

	public UcmFile getFileRevision(UcmSession s, UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return new UcmFile(this, file.getURI(), getFileRevision(s, file.getRevisionId(), true));
	}

	protected UcmAttributes getFileRevision(final UcmSession s, final String id, final boolean refreshRenditions)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		UcmUniqueURI guid = this.revisionUriByRevisionID.get(id);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> history = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> renditions = new AtomicReference<>(null);
		if (guid == null) {
			try {
				guid = this.revisionUriByRevisionID.createIfAbsent(id, new ConcurrentInitializer<UcmUniqueURI>() {
					@Override
					public UcmUniqueURI get() throws ConcurrentException {
						try {
							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.callService("DOC_INFO", new RequestPreparation() {
									@Override
									public void prepareRequest(DataBinder binder) {
										binder.putLocal("dID", id);
										if (refreshRenditions) {
											binder.putLocal("includeFileRenditionsInfo", "1");
										}
									}
								});
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the revision with ID [%s]",
									id)) { return UcmUniqueURI.NULL_GUID; }
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							// First things first!! Stash the retrieved object...
							DataResultSet rs = responseData.getResultSet("FileInfo");
							if (rs == null) { throw new UcmServiceException(
								String.format("Revision ID [%s] was found, but returned incorrect results?!?", id)); }

							Map<String, String> baseObj = new HashMap<>();
							baseObj.putAll(rs.getRows().get(0));
							// Capture the parent path...from DOC_INFO, it's stored in
							// LocalData.fParentPath
							baseObj.put(UcmAtt.cmfParentPath.name(), responseData.getLocalData().get("fParentPath"));

							DataObject docInfo = responseData.getResultSet("DOC_INFO").getRows().get(0);
							baseObj.putAll(docInfo);
							history.set(responseData.getResultSet("REVISION_HISTORY"));
							if (refreshRenditions) {
								renditions.set(responseData.getResultSet("Renditions"));
							}

							UcmAttributes baseData = new UcmAttributes(baseObj, rs.getFields());
							data.set(baseData);
							return UcmModel.getUniqueURI(baseData);
						} catch (IdcClientException | UcmException e) {
							throw new ConcurrentException(e);
						}
					}
				});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				throw new UcmServiceException(String.format("Exception caught locating revision ID [%s]", id), cause);
			}
		}

		if (UcmUniqueURI.NULL_GUID.equals(
			guid)) { throw new UcmFileRevisionNotFoundException(String.format("No revision found with ID [%s]", id)); }

		UcmAttributes ret = createIfAbsentInCache(this.objectByUniqueURI, guid,
			new ConcurrentInitializer<UcmAttributes>() {
				@Override
				public UcmAttributes get() throws ConcurrentException {
					UcmAttributes ret = data.get();
					cacheDataObject(ret);
					return ret;
				}
			});

		URI uri = UcmModel.getURI(ret);

		if (history.get() != null) {
			DataResultSet rs = history.get();
			LinkedList<UcmRevision> list = new LinkedList<>();
			for (DataObject o : rs.getRows()) {
				list.addFirst(new UcmRevision(uri, o, rs.getFields()));
			}
			this.historyByURI.put(uri, Tools.freezeList(new ArrayList<>(list)));
		}

		if (refreshRenditions && (renditions.get() != null)) {
			DataResultSet rs = renditions.get();
			Map<String, UcmRenditionInfo> m = new TreeMap<>();
			for (DataObject o : rs.getRows()) {
				UcmRenditionInfo r = new UcmRenditionInfo(guid, o, rs.getFields());
				m.put(r.getType().toUpperCase(), r);
			}
			this.renditionsByUniqueURI.put(guid, Tools.freezeMap(new LinkedHashMap<>(m)));
		}

		return ret;
	}

	protected Map<String, URI> getChildren(UcmSession s, String path)
		throws UcmServiceException, UcmFolderNotFoundException {
		try {
			return getFolderContents(s, resolvePath(s, path));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	protected Map<String, URI> getFolderContents(UcmSession s, UcmUniqueURI guid)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(guid, "Must provide a BY_GUID to search for");
		// If it's a folder URI we already know the SSP is the BY_GUID, so... go!
		return getFolderContents(s, guid.getURI());
	}

	public static interface ObjectHandler {
		public void handleObject(UcmSession session, int pos, URI objectUri, UcmFSObject object);
	}

	public int iterateFolderContents(final UcmSession s, final UcmFolder folder, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(folder, "Must provide a folder object to iterate over");
		return iterateFolderContents(s, folder.getURI(), handler);
	}

	int iterateFolderContents(final UcmSession s, final URI uri, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to search for");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");
		// If this isn't a folder, we don't even try it...
		if (!UcmModel.isFolderURI(uri)) { return -1; }

		Map<String, URI> children = this.childrenByURI.get(uri);
		boolean reconstruct = false;
		if (children != null) {
			// We'll gather the objects first, and then iterate over them, because
			// if there's an inconsistency (i.e. a missing stale object), then we
			// want to do the full service invocation to the server
			Map<URI, UcmAttributes> objects = new LinkedHashMap<>(children.size());
			for (URI childUri : children.values()) {
				try {
					objects.put(childUri, getDataObject(s, childUri));
				} catch (UcmObjectNotFoundException e) {
					reconstruct = true;
					break;
				}
			}

			// If the cache remains current, and we're not missing any child objects, then we quite
			// simply iterate over the objects we got and call it a day
			if (!reconstruct) {
				int ret = 0;
				for (URI childUri : objects.keySet()) {
					handler.handleObject(s, ret++, childUri, newFSObject(childUri, objects.get(childUri)));
				}
				return ret;
			}
		}

		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		final AtomicReference<Map<String, UcmAttributes>> rawChildren = new AtomicReference<>(null);
		if (children == null) {
			try {
				children = this.childrenByURI.createIfAbsent(uri, new ConcurrentInitializer<Map<String, URI>>() {
					@Override
					public Map<String, URI> get() throws ConcurrentException {
						try {
							try {
								Map<String, URI> children = new TreeMap<>();
								Map<String, UcmAttributes> dataObjects = new TreeMap<>();
								FolderContentsIterator it = new FolderContentsIterator(s, uri);
								while (it.hasNext()) {
									UcmAttributes o = it.next();
									URI childUri = UcmModel.getURI(o);
									String name = o.getString(UcmAtt.fFileName);
									if (name == null) {
										name = o.getString(UcmAtt.fFolderName);
									}
									children.put(name, childUri);
									dataObjects.put(name, o);
									handler.handleObject(s, it.getCurrentPos(), childUri, newFSObject(childUri, o));
								}
								rawChildren.set(dataObjects);
								data.set(it.getFolder());
								return children;
							} catch (final UcmServiceException e) {
								Throwable cause = e.getCause();
								if (isNotFoundException(cause, "Exception caught retrieving the URI [%s]",
									uri)) { throw new UcmFolderNotFoundException(
										String.format("No folder found with URI [%s]", uri)); }
								// This is a "regular" exception that we simply re-raise
								throw e;
							}
						} catch (UcmException e) {
							throw new ConcurrentException(e);
						}
					}
				});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				UcmModel.throwIfMatches(UcmFolderNotFoundException.class, cause);
				throw new UcmServiceException(
					String.format("Exception caught finding the folder contents for URI [%s]", uri), cause);
			}
		}

		if (data.get() != null) {
			cacheDataObject(data.get());
		}

		if (rawChildren.get() != null) {
			Map<String, UcmAttributes> c = rawChildren.get();
			for (String name : c.keySet()) {
				cacheDataObject(c.get(name));
			}
		}

		return (children != null ? children.size() : 0);
	}

	protected Map<String, URI> getFolderContents(UcmSession s, final URI uri)
		throws UcmServiceException, UcmFolderNotFoundException {
		final Map<String, URI> children = new LinkedHashMap<>();
		iterateFolderContents(s, uri, new ObjectHandler() {
			@Override
			public void handleObject(UcmSession session, int pos, URI uri, UcmFSObject object) {
				children.put(object.getName(), uri);
			}
		});
		return children;
	}

	public Map<String, UcmFSObject> getFolderContents(UcmSession s, final UcmFolder folder)
		throws UcmServiceException, UcmFolderNotFoundException {
		final Map<String, UcmFSObject> children = new LinkedHashMap<>();
		iterateFolderContents(s, folder.getURI(), new ObjectHandler() {
			@Override
			public void handleObject(UcmSession session, int pos, URI uri, UcmFSObject data) {
				children.put(data.getName(), data);
			}
		});
		return children;
	}

	private int iterateFolderContentsRecursive(final Set<URI> recursions, final AtomicInteger outerPos,
		final UcmSession s, final URI uri, final boolean followShortcuts, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to search for");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");
		// If this isn't a folder, we don't even try it...
		if (!UcmModel.isFolderURI(uri)) { return -1; }

		if (recursions.isEmpty()) {
			// If this is the root of the invocation, we handle it!
			handler.handleObject(s, outerPos.getAndIncrement(), uri, getFolder(s, uri));
		}

		if (!recursions.add(uri)) { throw new IllegalStateException(
			String.format("Folder recursion detected when descending into [%s] : %s", uri, recursions)); }

		try {
			iterateFolderContents(s, uri, new ObjectHandler() {
				@Override
				public void handleObject(UcmSession session, int pos, URI objectUri, UcmFSObject object) {
					handler.handleObject(session, outerPos.getAndIncrement(), objectUri, object);
					if (UcmModel.isFolderURI(uri) && (followShortcuts || !object.isShortcut())) {
						try {
							iterateFolderContentsRecursive(recursions, outerPos, s, objectUri, followShortcuts,
								handler);
						} catch (UcmFolderNotFoundException e) {
							throw new UcmRuntimeException(String.format(
								"Unexpected condition: can't find a folder that has just been found?? URI=[%s]",
								objectUri), e);
						} catch (UcmServiceException e) {
							throw new UcmRuntimeServiceException(
								String.format("Service exception caught while attempting to recurse through [%s] : %s",
									objectUri, recursions),
								e);
						}
					}
				}
			});
			return outerPos.get();
		} catch (UcmRuntimeServiceException e) {
			throw new UcmServiceException(String.format("Service exception caught while recursing through [%s]", uri),
				e);
		} finally {
			recursions.remove(uri);
		}
	}

	public int iterateFolderContentsRecursive(final UcmSession s, final UcmFolder folder, boolean followShortCuts,
		final ObjectHandler handler) throws UcmServiceException, UcmFolderNotFoundException {
		return iterateFolderContentsRecursive(s, folder.getURI(), followShortCuts, handler);
	}

	int iterateFolderContentsRecursive(final UcmSession s, final URI uri, boolean followShortCuts,
		final ObjectHandler handler) throws UcmServiceException, UcmFolderNotFoundException {
		return iterateFolderContentsRecursive(new LinkedHashSet<URI>(), new AtomicInteger(0), s, uri, followShortCuts,
			handler);
	}

	Collection<URI> getFolderContentsRecursive(UcmSession s, boolean followShortCuts, final URI uri)
		throws UcmServiceException, UcmFolderNotFoundException {
		final Collection<URI> children = new ArrayList<>();
		iterateFolderContentsRecursive(s, uri, followShortCuts, new ObjectHandler() {
			@Override
			public void handleObject(UcmSession session, int pos, URI uri, UcmFSObject obj) {
				children.add(uri);
			}
		});
		return children;
	}

	public Collection<UcmFSObject> getFolderContentsRecursive(UcmSession s, final UcmFolder folder,
		boolean followShortCuts) throws UcmServiceException, UcmFolderNotFoundException {
		final Collection<UcmFSObject> children = new ArrayList<>();
		iterateFolderContentsRecursive(s, folder.getURI(), followShortCuts, new ObjectHandler() {
			@Override
			public void handleObject(UcmSession session, int pos, URI uri, UcmFSObject o) {
				children.add(o);
			}
		});
		return children;
	}

	public UcmFolder getRootFolder(UcmSession s) throws UcmServiceException {
		try {
			return getFolder(s, UcmModel.ROOT_URI);
		} catch (UcmFolderNotFoundException e) {
			throw new UcmServiceException("Could not find the root folder - this appears to be a server problem", e);
		}
	}

	public UcmFolder getFolder(UcmSession s, UcmUniqueURI uri) throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a unique URI to locate");
		return getFolder(s, uri.getURI());
	}

	public UcmFolder getFolder(UcmSession s, String path) throws UcmServiceException, UcmFolderNotFoundException {
		URI uri = null;
		try {
			uri = resolvePath(s, path);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
		return getFolder(s, uri);
	}

	public UcmFolder getFolder(UcmSession s, URI uri) throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to locate");
		if (!UcmModel.isFolderURI(uri)) { throw new UcmFolderNotFoundException(
			String.format("The object URI [%s] is not a folder URI", uri)); }
		try {
			return new UcmFolder(this, uri, getDataObject(s, uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFileHistory getFileHistory(UcmSession s, String path)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileHistory(s, getFile(s, path));
	}

	public UcmFileHistory getFileHistory(UcmSession s, UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileHistory(s, file.getURI(), file.getRevisionId());
	}

	UcmFileHistory getFileHistory(UcmSession s, URI uri)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		final UcmAttributes att;
		try {
			att = getDataObject(s, uri);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}

		return getFileHistory(s, uri, att.getString(UcmAtt.dID));
	}

	UcmFileHistory getFileHistory(final UcmSession s, final URI uri, final String revisionId)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		List<UcmRevision> history = this.historyByURI.get(uri);
		if (history == null) {
			try {
				history = this.historyByURI.createIfAbsent(uri, new ConcurrentInitializer<List<UcmRevision>>() {
					@Override
					public List<UcmRevision> get() throws ConcurrentException {
						try {
							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.callService("REV_HISTORY", new RequestPreparation() {
									@Override
									public void prepareRequest(DataBinder binder) {
										binder.putLocal("dID", revisionId);
									}
								});
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the URI [%s]",
									uri)) { throw new UcmFolderNotFoundException(
										String.format("No file found with URI [%s]", uri)); }
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							DataResultSet revisions = responseData.getResultSet("REVISIONS");
							LinkedList<UcmRevision> info = new LinkedList<>();
							for (DataObject o : revisions.getRows()) {
								info.addFirst(new UcmRevision(uri, o, revisions.getFields()));
							}
							return info;
						} catch (IdcClientException | UcmException e) {
							throw new ConcurrentException(e);
						}
					}
				});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				UcmModel.throwIfMatches(UcmFileNotFoundException.class, cause);
				throw new UcmServiceException(
					String.format("Exception caught finding the file history for URI [%s]", uri), cause);
			}
		}

		return new UcmFileHistory(this, uri, history);
	}

	public InputStream getInputStream(UcmSession s, final UcmFile file, final String rendition)
		throws UcmServiceException, UcmFileRevisionNotFoundException, UcmRenditionNotFoundException {
		ServiceResponse response = s.callService("GET_FILE", new RequestPreparation() {
			@Override
			public void prepareRequest(DataBinder binder) {
				binder.putLocal("dID", file.getRevisionId());
				if (!StringUtils.isEmpty(rendition)) {
					binder.putLocal("Rendition", rendition.toUpperCase());
				}
			}
		});
		if (response.getResponseType() == ResponseType.STREAM) { return response.getResponseStream(); }

		// Ok...there was a problem... what was the problem?
		final DataBinder binder;
		try {
			binder = response.getResponseAsBinder();
		} catch (IdcClientException e) {
			throw new UcmServiceException(String.format(
				"Failed to decode the service response when invoking GET_FILE for rendition [%s] from file [%s]",
				Tools.coalesce(rendition, UcmModel.PRIMARY), file.getUniqueURI()), e);
		}

		DataObject local = binder.getLocalData();
		int status = local.getInteger("StatusCode");
		if (status == -16) {
			// Resource not found...so the revision wasn't found
			throw new UcmFileRevisionNotFoundException(
				String.format("File revision [%s] was not found", file.getUniqueURI()));
		}

		if (status == -32) {
			// Procedural error - so the revision was found, but not the rendition...maybe?
			if (local.get("StatusMessageKey").indexOf("!csGetFileRenditionNotFound,") >= 0) {
				// Rendition not found!
				throw new UcmRenditionNotFoundException(String.format("Rendition [%s] not found for file [%s]",
					Tools.coalesce(rendition, UcmModel.PRIMARY), file.getUniqueURI()));
			}
		}

		// Some other error!
		throw new UcmServiceException(String.format("Failed to load rendition [%s] from file [%s]",
			Tools.coalesce(rendition, UcmModel.PRIMARY), file.getUniqueURI()));
	}

	static String sanitizePath(String path) {
		Objects.requireNonNull(path, "Must provide a non-null path");
		if (!UcmModel.PATH_CHECKER.matcher(path).matches()) {
			// Single separators...
			path = path.replaceAll("/+", "/");
			// Make sure there's a leading separator
			if (!path.startsWith("/")) {
				path = String.format("/%s", path);
			}
		}
		String newPath = FilenameUtils.normalizeNoEndSeparator(path, true);
		if (newPath == null) { throw new IllegalArgumentException(
			String.format("The given path [%s] is invalid - too may '..' elements", path)); }
		return newPath;
	}

	static <T extends Throwable> void throwIfMatches(Class<T> k, Throwable t) throws T {
		Objects.requireNonNull(k, "Must provide an exception class to evaluate");
		if (k.isInstance(t)) { throw k.cast(t); }
	}

	UcmFile refresh(UcmSession s, UcmFile f)
		throws UcmFileNotFoundException, UcmServiceException, UcmFileRevisionNotFoundException {
		return getFileRevision(s, f);
	}

	UcmFolder refresh(UcmSession s, UcmFolder f) throws UcmFolderNotFoundException, UcmServiceException {
		return getFolder(s, f.getUniqueURI());
	}

	UcmFileHistory refresh(UcmSession s, UcmFileHistory h)
		throws UcmFileNotFoundException, UcmServiceException, UcmFileRevisionNotFoundException {
		return getFileHistory(s, h.getURI());
	}

	public Map<String, UcmRenditionInfo> getRenditions(final UcmSession s, final UcmFile file)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		Objects.requireNonNull(file, "Must provide a file whose renditions to return");

		final UcmUniqueURI guid = file.getUniqueURI();
		Map<String, UcmRenditionInfo> renditions = this.renditionsByUniqueURI.get(guid);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> history = new AtomicReference<>(null);
		if (renditions == null) {
			final String id = file.getRevisionId();
			try {
				renditions = this.renditionsByUniqueURI.createIfAbsent(guid,
					new ConcurrentInitializer<Map<String, UcmRenditionInfo>>() {
						@Override
						public Map<String, UcmRenditionInfo> get() throws ConcurrentException {
							try {
								ServiceResponse response = null;
								DataBinder responseData = null;
								try {
									response = s.callService("DOC_INFO", new RequestPreparation() {
										@Override
										public void prepareRequest(DataBinder binder) {
											binder.putLocal("dID", id);
											binder.putLocal("includeFileRenditionsInfo", "1");
										}
									});
									responseData = response.getResponseAsBinder();
								} catch (final IdcClientException e) {
									if (isNotFoundException(e, "Exception caught retrieving the revision with ID [%s]",
										id)) { throw new UcmFileRevisionNotFoundException(); }
									// This is a "regular" exception that we simply re-raise
									throw e;
								}

								// First things first!! Stash the retrieved object...
								DataResultSet rs = responseData.getResultSet("FileInfo");
								if (rs == null) { throw new UcmServiceException(String
									.format("Revision ID [%s] was found, but returned incorrect results?!?", id)); }

								Map<String, String> baseObj = new HashMap<>();
								baseObj.putAll(rs.getRows().get(0));
								// Capture the parent path...from DOC_INFO, it's stored in
								// LocalData.fParentPath
								baseObj.put(UcmAtt.cmfParentPath.name(),
									responseData.getLocalData().get("fParentPath"));

								DataResultSet DOC_INFO = responseData.getResultSet("DOC_INFO");
								DataObject docInfo = DOC_INFO.getRows().get(0);
								baseObj.putAll(docInfo);
								history.set(responseData.getResultSet("REVISION_HISTORY"));

								Map<String, UcmRenditionInfo> renditions = new TreeMap<>();
								rs = responseData.getResultSet("Renditions");
								if (rs == null) { throw new UcmServiceException(String.format(
									"Revision ID [%s] was found, but no rendition information was returned??!", id)); }
								for (DataObject o : rs.getRows()) {
									UcmRenditionInfo r = new UcmRenditionInfo(guid, o, rs.getFields());
									renditions.put(r.getType().toUpperCase(), r);
								}

								UcmAttributes baseData = new UcmAttributes(baseObj, DOC_INFO.getFields());
								data.set(baseData);
								return renditions;
							} catch (IdcClientException | UcmException e) {
								throw new ConcurrentException(e);
							}
						}
					});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				UcmModel.throwIfMatches(UcmFileRevisionNotFoundException.class, cause);
				throw new UcmServiceException(
					String.format("Exception caught retrieving the renditions list for file [%s] (revision ID [%s]",
						file.getURI(), file.getRevisionId()),
					cause);
			}
		}

		// Update the base object, since we just got it anyhow...
		if (data.get() != null) {
			createIfAbsentInCache(this.objectByUniqueURI, guid, new ConcurrentInitializer<UcmAttributes>() {
				@Override
				public UcmAttributes get() throws ConcurrentException {
					UcmAttributes ret = data.get();
					cacheDataObject(ret);
					return ret;
				}
			});
		}

		if (history.get() != null) {
			DataResultSet rs = history.get();
			LinkedList<UcmRevision> list = new LinkedList<>();
			for (DataObject o : rs.getRows()) {
				list.addFirst(new UcmRevision(file.getURI(), o, rs.getFields()));
			}
			this.historyByURI.put(file.getURI(), Tools.freezeList(new ArrayList<>(list)));
		}
		return new TreeMap<>(renditions);
	}
}