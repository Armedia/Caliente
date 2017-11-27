package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.io.Serializable;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.engine.ucm.UcmConstants;
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
// CHECKIN_UNIVERSAL (dDocAuthor, dDocTitle, dDocType, dSecurityGroup, dCreateDate, doFileCopy = 1)
public class UcmModel {
	// Syntax: query{sortAtts}[startRow,rowCount] / sortAtts == +att1,-att2,
	private static final Pattern QUERY_PARSER = Pattern
		.compile("^(.*?)(?:\\s*\\{\\s*(.*)\\s*\\}\\s*)?(?:\\s*\\[\\s*(.*)\\s*\\]\\s*)?$", Pattern.DOTALL);
	private static final Pattern SORT_PARSER = Pattern.compile("^[-+]?\\w+$");
	private static final Pattern ROW_PARSER = Pattern
		.compile("^(?:\\s*([1-9][0-9]*)\\s*,)?\\s*([1-9][0-9]*)\\s*(?:/\\s*([1-9][0-9]*))?\\s*$");
	private static final Pattern PATH_CHECKER = Pattern.compile("^(/|(/[^/]+)+/?)$");
	private static final String RENDITION_DEFAULT_TYPE = UcmRenditionInfo.DEFAULT;
	private static final String REMDITION_DEFAULT_FORMAT = "application/octet-stream";
	private static final int MIN_OBJECT_COUNT = 100;
	private static final int DEFAULT_OBJECT_COUNT = 10000;
	private static final int MAX_OBJECT_COUNT = 1000000;

	private static final String FILE_SCHEME = "file";
	private static final String FOLDER_SCHEME = "folder";
	private static final String NULL_SCHEME = "null";

	private static final URI NULL_URI = UcmModel.newURI(UcmModel.NULL_SCHEME, "null");
	private static final String NULL_FOLDER_GUID = "idcnull";
	static final URI NULL_FOLDER_URI = UcmModel.newFolderURI(UcmModel.NULL_FOLDER_GUID);

	static final URI ROOT_URI = UcmModel.newFolderURI("FLD_ROOT");

	// UniqueURI:
	// * FILE -> file:${dDocName}#${dID}
	// * FOLDER -> folder:${fFolderGUID}

	// Unique URI -> UcmAttributes
	private final KeyLockableCache<UcmUniqueURI, UcmFSObject> objectByUniqueURI;

	// path -> HistoryURI
	private final KeyLockableCache<String, URI> uriByPaths;

	// GUID -> HistoryURI
	private final KeyLockableCache<String, URI> uriByGUID;

	// Child HistoryURI -> Parent HistoryURI
	private final KeyLockableCache<URI, URI> parentByURI;

	// Parent HistoryURI -> Map<Child Name, Child HistoryURI>
	private final KeyLockableCache<URI, Map<String, URI>> childrenByURI;

	// HistoryURI -> List<UcmRevision>
	private final KeyLockableCache<URI, List<UcmRevision>> historyByURI;

	// dID -> UcmUniqueURI
	private final KeyLockableCache<String, UcmUniqueURI> revisionUriByRevisionID;

	// UcmUniqueURI -> Map<String, UcmRenditionInfo>
	private final KeyLockableCache<UcmUniqueURI, Map<String, UcmRenditionInfo>> renditionsByUniqueURI;

	// HistoryURI -> UcmUniqueURI
	private final KeyLockableCache<URI, UcmUniqueURI> uniqueUriByHistoryUri;

	// UniqueURI -> HistoryURI
	private final KeyLockableCache<UcmUniqueURI, URI> historyUriByUniqueURI;

	public static DataBinder getConfigInfo(UcmSession s) throws UcmServiceException {
		try {
			ServiceResponse response = s.callService("CONFIG_INFO");
			return response.getResponseAsBinder();
		} catch (final IdcClientException e) {
			if (ServiceException.class.isInstance(e)) {
				ServiceException se = ServiceException.class.cast(e);

				DataBinder binder = se.getBinder();
				DataObject local = binder.getLocalData();

				int statusCode = local.getInteger("StatusCode");
				if (statusCode == -1) {
					String mk = local.get("StatusMessageKey");
					List<UcmExceptionData.Entry> entries = UcmExceptionData.parseMessageKey(mk);
					if (entries.size() == 2) {
						UcmExceptionData.Entry first = entries.get(0);
						UcmExceptionData.Entry second = entries.get(1);
						if (first.tagIs("csUnableToRetrieveConfigInfo") && second.tagIs("csUserInsufficientAccess")) {
							// This user lacks access to CONFIG_INFO, so just return null
							// instead of an error, since this allows the caller to decide what
							// to do about this
							return null;
						}
					}
				}
			}

			throw new UcmServiceException("Failed to retrieve the system configuration information using CONFIG_INFO",
				e);
		}
	}

	public static Boolean isFrameworkFoldersEnabled(UcmSession s) throws UcmServiceException {
		DataBinder responseData = UcmModel.getConfigInfo(s);
		if (responseData == null) { return null; }

		// First things first!! Stash the retrieved object...
		DataResultSet rs = responseData.getResultSet("EnabledComponents");
		List<DataObject> components = rs.getRows();
		for (DataObject component : components) {
			if (("FrameworkFolders".equals(component.get("name")))
				&& ("Enabled".equals(component.get("status")))) { return Boolean.TRUE; }
		}
		return Boolean.FALSE;
	}

	public static boolean isRoot(URI uri) {
		return UcmModel.ROOT_URI.equals(uri);
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

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

	protected void cacheDataObject(final UcmFSObject object) {
		if (object == null) { return; }
		// Is this a file or a folder?
		final URI uri = object.getURI();
		final UcmUniqueURI guid = object.getUniqueURI();

		this.objectByUniqueURI.put(guid, object);

		if (object.hasAttribute(UcmAtt.fParentGUID)) {
			this.parentByURI.put(uri, UcmModel.newFolderURI(object.getString(UcmAtt.fParentGUID)));
		}

		this.uniqueUriByHistoryUri.put(uri, guid);
		this.historyUriByUniqueURI.put(guid, uri);
	}

	protected <K extends Serializable, V> V createIfAbsentInCache(KeyLockableCache<K, V> cache, K key,
		ConcurrentInitializer<V> initializer) {
		try {
			return cache.createIfAbsent(key, initializer);
		} catch (Exception e) {
			// Never gonna happen...
			throw new UcmRuntimeException("Unexpected Exception", e);
		}
	}

	protected static final URI getURI(UcmAttributes data) {
		if (data.hasAttribute(UcmAtt.dDocName)) { return UcmModel.newFileURI(data.getString(UcmAtt.dDocName)); }
		if (data.hasAttribute(UcmAtt.fFolderGUID)) { return UcmModel.newFolderURI(data.getString(UcmAtt.fFolderGUID)); }
		throw new UcmRuntimeException(
			String.format("Could not find either dDocName or fFolderGUID in the given attribute set: %s", data));
	}

	protected static final UcmUniqueURI getUniqueURI(UcmAttributes data) {
		if (data == null) { return null; }
		URI uri = UcmModel.getURI(data);
		if (uri == null) { return null; }
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
		if (StringUtils.isEmpty(ssp)) { return null; }
		return UcmModel.newURI(UcmModel.FILE_SCHEME, ssp, null);
	}

	protected static final URI newFolderURI(String ssp) {
		if (StringUtils.isEmpty(ssp)) { return null; }
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

	private UcmAttributes buildAttributesFromDocInfo(DataBinder responseData, AtomicReference<DataResultSet> history,
		AtomicReference<DataResultSet> renditions) throws UcmServiceException {
		Map<String, String> baseObj = new HashMap<>();
		// First things first!! Stash the retrieved object...
		DataResultSet docInfo = responseData.getResultSet("DOC_INFO");
		if (docInfo == null) { return null; }

		DataResultSet fileInfo = responseData.getResultSet("FileInfo");
		if (fileInfo != null) {
			baseObj.putAll(fileInfo.getRows().get(0));
		}

		String parentPath = responseData.getLocalData().get("fParentPath");
		if (!StringUtils.isEmpty(parentPath)) {
			// Capture the parent path...from DOC_INFO, it's stored in LocalData.fParentPath
			baseObj.put(UcmAtt.cmfParentPath.name(), responseData.getLocalData().get("fParentPath"));
		}

		baseObj.putAll(docInfo.getRows().get(0));

		if (history != null) {
			history.set(responseData.getResultSet("REVISION_HISTORY"));
		}
		if (renditions != null) {
			renditions.set(responseData.getResultSet("Renditions"));
		}

		UcmAttributes baseData = new UcmAttributes(baseObj, docInfo.getFields(),
			(fileInfo != null ? fileInfo.getFields() : null));
		return baseData;
	}

	private UcmAttributes buildAttributesFromFldInfo(DataBinder responseData) {
		boolean file = true;
		DataResultSet rs = responseData.getResultSet("FileInfo");
		if (rs == null) {
			file = false;
			rs = responseData.getResultSet("FolderInfo");
		}
		if (rs == null) { return null; }

		Map<String, String> baseObj = new HashMap<>();
		baseObj.putAll(rs.getRows().get(0));
		// Capture the parent path - it's either LocalData.filePath or LocalData.folderPath...but it
		// also contains the filename so we need to dirname it
		String parentPath = responseData.getLocal(String.format("%sPath", file ? "file" : "folder"));
		if (!StringUtils.isEmpty(parentPath)) {
			baseObj.put(UcmAtt.cmfParentPath.name(), FileNameTools.dirname(parentPath, '/'));
		}
		return new UcmAttributes(baseObj, rs.getFields());
	}

	public UcmFSObject getObject(UcmSession s, String path) throws UcmServiceException, UcmObjectNotFoundException {
		final URI uri = resolvePath(s, path);
		return getFSObject(s, uri);
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
			UcmFSObject obj = getFSObject(s, uri);
			if (!UcmFile.class.isInstance(obj)) { throw new UcmFileNotFoundException(
				String.format("The file with URI [%s] does not exist", uri)); }
			return UcmFile.class.cast(obj);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFile getFileByGUID(UcmSession s, String guid) throws UcmServiceException, UcmFileNotFoundException {
		try {
			URI uri = resolveGuid(s, guid, UcmObjectType.FILE);
			UcmFSObject obj = getFSObject(s, uri);
			if (!UcmFile.class.isInstance(obj)) { throw new UcmFileNotFoundException(
				String.format("The file with URI [%s] does not exist", uri)); }
			return UcmFile.class.cast(obj);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFolder getFolderByGUID(UcmSession s, String guid) throws UcmServiceException, UcmFolderNotFoundException {
		try {
			URI uri = resolveGuid(s, guid, UcmObjectType.FOLDER);
			UcmFSObject obj = getFSObject(s, uri);
			if (!UcmFolder.class.isInstance(obj)) { throw new UcmFileNotFoundException(
				String.format("The folder with URI [%s] does not exist", uri)); }
			return UcmFolder.class.cast(obj);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	protected UcmFSObject newFSObject(URI uri, UcmAttributes att) {
		return UcmModel.isFileURI(uri) ? new UcmFile(this, uri, att) : new UcmFolder(this, uri, att);
	}

	protected UcmFSObject newFSObject(UcmAttributes att) {
		return newFSObject(UcmModel.getURI(att), att);
	}

	protected URI resolvePath(final UcmSession s, String p) throws UcmServiceException, UcmObjectNotFoundException {
		final String sanitizedPath = UcmModel.sanitizePath(p);
		URI uri = this.uriByPaths.get(sanitizedPath);
		final AtomicReference<UcmFSObject> data = new AtomicReference<>(null);
		final AtomicReference<Throwable> thrown = new AtomicReference<>(null);
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
									sanitizedPath)) {
									thrown.set(e);
									return UcmModel.NULL_URI;
								}
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							final UcmAttributes attributes = buildAttributesFromFldInfo(responseData);
							if (attributes == null) { throw new UcmServiceException(String.format(
								"Path [%s] was found via FLD_INFO, but was neither a file nor a folder?!?",
								sanitizedPath)); }
							URI uri = UcmModel.getURI(attributes);
							data.set(newFSObject(uri, attributes));
							if (uri == null) { throw new UcmServiceException(String.format(
								"Path [%s] was found, but was neither a file nor a folder (no identifier attributes)?!?",
								sanitizedPath)); }
							return uri;
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

		if (Tools.equals(UcmModel.NULL_URI,
			uri)) { throw new UcmObjectNotFoundException(String.format("No object found at path [%s]", sanitizedPath),
				thrown.get()); }
		return uri;
	}

	protected URI resolveGuid(final UcmSession s, final String guid, final UcmObjectType type)
		throws UcmServiceException, UcmObjectNotFoundException {
		URI uri = this.uriByGUID.get(guid);
		final AtomicReference<UcmFSObject> data = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> history = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> renditions = new AtomicReference<>(null);
		final AtomicReference<Throwable> thrown = new AtomicReference<>(null);
		if (uri == null) {
			try {
				uri = this.uriByPaths.createIfAbsent(guid, new ConcurrentInitializer<URI>() {
					@Override
					public URI get() throws ConcurrentException {
						try {
							ServiceResponse response = null;
							DataBinder responseData = null;
							final UcmAtt identifierAtt;
							final String serviceName;
							switch (type) {
								case FILE:
									identifierAtt = UcmAtt.dDocName;
									serviceName = "DOC_INFO_BY_NAME";
									break;
								case FOLDER:
									identifierAtt = UcmAtt.fFolderGUID;
									serviceName = "FLD_INFO";
									break;

								default:
									throw new UcmServiceException(
										String.format("Unsupported object type %s", type.name()));
							}

							try {
								response = s.callService(serviceName, new RequestPreparation() {
									@Override
									public void prepareRequest(DataBinder binder) {
										binder.putLocal(identifierAtt.name(), guid);
										if (type == UcmObjectType.FILE) {
											binder.putLocal("includeFileRenditionsInfo", "1");
											binder.putLocal("isAddFolderMetadata", "1");
										}
									}
								});
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the %s with GUID [%s]",
									type.name(), guid)) {
									thrown.set(e);
									return UcmModel.NULL_URI;
								}
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							final UcmAttributes attributes;
							if (type == UcmObjectType.FILE) {
								attributes = buildAttributesFromDocInfo(responseData, history, renditions);
							} else {
								attributes = buildAttributesFromFldInfo(responseData);
							}
							if (attributes == null) { throw new UcmServiceException(
								String.format("%s GUID [%s] was found via %s(%s=%s), didn't contain any data?!?",
									type.name(), guid, serviceName, identifierAtt.name(), guid)); }
							String uriIdentifier = data.get().getString(identifierAtt);
							if (uriIdentifier != null) {
								URI uri = UcmModel.getURI(attributes);
								data.set(newFSObject(uri, attributes));
								return uri;
							}

							throw new UcmServiceException(
								String.format("%s GUID [%s] was found, returned no results (no value for %s)?!?",
									type.name(), guid, identifierAtt.name()));
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
		if (data.get() != null) {
			final UcmFSObject object = data.get();
			cacheDataObject(object);

			if (history.get() != null) {
				DataResultSet rs = history.get();
				LinkedList<UcmRevision> list = new LinkedList<>();
				for (DataObject o : rs.getRows()) {
					list.addFirst(new UcmRevision(uri, o, rs.getFields()));
				}
				this.historyByURI.put(uri, Tools.freezeList(new ArrayList<>(list)));
			}

			if (renditions.get() != null) {
				UcmUniqueURI uniqueId = object.getUniqueURI();
				DataResultSet rs = renditions.get();
				Map<String, UcmRenditionInfo> m = new TreeMap<>();
				for (DataObject o : rs.getRows()) {
					UcmRenditionInfo r = new UcmRenditionInfo(uniqueId, o, rs.getFields());
					m.put(r.getType().toUpperCase(), r);
				}
				this.renditionsByUniqueURI.put(uniqueId, Tools.freezeMap(new LinkedHashMap<>(m)));
			}
		}

		if (Tools.equals(UcmModel.NULL_URI,
			uri)) { throw new UcmObjectNotFoundException(String.format("No %s found with GUID [%s]", type.name(), guid),
				thrown.get()); }
		return uri;
	}

	protected UcmFSObject getFSObject(final UcmSession s, final URI uri)
		throws UcmServiceException, UcmObjectNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to retrieve");
		if (UcmModel.NULL_FOLDER_GUID.equals(uri.getSchemeSpecificPart())) {
			// Take a quick shortcut to avoid unnecessary calls
			return null;
		}

		final boolean file;
		if (UcmModel.isFileURI(uri)) {
			// The SSP is the dDocName
			file = true;
		} else if (UcmModel.isFolderURI(uri)) {
			// The SSP is the fFolderGUID
			file = false;
		} else {
			// WTF?? Invalid URI
			throw new IllegalArgumentException(String.format("The URI [%s] doesn't point to a valid object", uri));
		}

		final AtomicBoolean serviceInvoked = new AtomicBoolean(false);
		final AtomicReference<DataResultSet> history = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> renditions = new AtomicReference<>(null);
		final AtomicReference<Throwable> thrown = new AtomicReference<>(null);
		UcmUniqueURI guid = this.uniqueUriByHistoryUri.get(uri);
		if (guid == null) {
			try {
				guid = this.uniqueUriByHistoryUri.createIfAbsent(uri, new ConcurrentInitializer<UcmUniqueURI>() {
					@Override
					public UcmUniqueURI get() throws ConcurrentException {
						try {
							ServiceResponse response = null;
							DataBinder responseData = null;
							final UcmAtt identifierAtt;
							final String serviceName;
							final String searchKey;
							if (file) {
								String id = uri.getFragment();
								if (id != null) {
									serviceName = "DOC_INFO";
									identifierAtt = UcmAtt.dID;
									searchKey = id;
								} else {
									serviceName = "DOC_INFO_BY_NAME";
									identifierAtt = UcmAtt.dDocName;
									searchKey = uri.getSchemeSpecificPart();
								}
							} else {
								identifierAtt = UcmAtt.fFolderGUID;
								serviceName = "FLD_INFO";
								searchKey = uri.getSchemeSpecificPart();
							}

							try {
								response = s.callService(serviceName, new RequestPreparation() {
									@Override
									public void prepareRequest(DataBinder binder) {
										binder.putLocal(identifierAtt.name(), searchKey);
										if (file) {
											binder.putLocal("isAddFolderMetadata", "1");
										}
									}
								});
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught locating the object using URI [%s]",
									uri)) {
									thrown.set(e);
									return UcmUniqueURI.NULL_GUID;
								}
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							final UcmAttributes attributes;
							if (file) {
								attributes = buildAttributesFromDocInfo(responseData, history, renditions);
							} else {
								attributes = buildAttributesFromFldInfo(responseData);
							}
							if (attributes == null) { throw new UcmServiceException(
								String.format("The URI [%s] was found via %s(%s=%s), didn't contain any data?!?", uri,
									serviceName, identifierAtt.name(), searchKey)); }
							final URI uri = UcmModel.getURI(attributes);
							final UcmUniqueURI guid = UcmModel.getUniqueURI(attributes);
							final UcmFSObject object = newFSObject(uri, attributes);
							UcmModel.this.objectByUniqueURI.put(guid, object);
							if (attributes.hasAttribute(UcmAtt.fParentGUID)) {
								UcmModel.this.parentByURI.put(uri,
									UcmModel.newFolderURI(attributes.getString(UcmAtt.fParentGUID)));
							}
							UcmModel.this.historyUriByUniqueURI.put(guid, uri);
							serviceInvoked.set(true);
							return guid;
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

		if (UcmUniqueURI.NULL_GUID
			.equals(guid)) { throw new UcmObjectNotFoundException(String.format("No object found with URI [%s]", uri),
				thrown.get()); }

		if (serviceInvoked.get()) {
			if (history.get() != null) {
				DataResultSet rs = history.get();
				LinkedList<UcmRevision> list = new LinkedList<>();
				for (DataObject o : rs.getRows()) {
					list.addFirst(new UcmRevision(uri, o, rs.getFields()));
				}
				this.historyByURI.put(uri, Tools.freezeList(new ArrayList<>(list)));
			}

			if (renditions.get() != null) {
				DataResultSet rs = renditions.get();
				Map<String, UcmRenditionInfo> m = new TreeMap<>();
				for (DataObject o : rs.getRows()) {
					UcmRenditionInfo r = new UcmRenditionInfo(guid, o, rs.getFields());
					m.put(r.getType().toUpperCase(), r);
				}
				this.renditionsByUniqueURI.put(guid, Tools.freezeMap(new LinkedHashMap<>(m)));
			}
		}

		return this.objectByUniqueURI.get(guid);
	}

	public UcmFile getFileRevision(UcmSession s, UcmRevision revision)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileRevision(s, revision.getId());
	}

	public UcmFile getFileRevision(UcmSession s, UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileRevision(s, file.getRevisionId());
	}

	protected UcmFile getFileRevision(final UcmSession s, final String id)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		UcmUniqueURI guid = this.revisionUriByRevisionID.get(id);
		final AtomicBoolean serviceInvoked = new AtomicBoolean(false);
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
										binder.putLocal("includeFileRenditionsInfo", "1");
										binder.putLocal("isAddFolderMetadata", "1");
									}
								});
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the revision with ID [%s]",
									id)) { return UcmUniqueURI.NULL_GUID; }
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							UcmAttributes attributes = buildAttributesFromDocInfo(responseData, history, renditions);
							if (attributes == null) { throw new UcmServiceException(String.format(
								"Revision ID [%s] was found via DOC_INFO(dID=%s), but returned empty results", id,
								id)); }
							final URI uri = UcmModel.getURI(attributes);
							final UcmUniqueURI guid = UcmModel.getUniqueURI(attributes);
							UcmModel.this.objectByUniqueURI.put(guid, newFSObject(uri, attributes));
							if (attributes.hasAttribute(UcmAtt.fParentGUID)) {
								UcmModel.this.parentByURI.put(uri,
									UcmModel.newFolderURI(attributes.getString(UcmAtt.fParentGUID)));
							}
							UcmModel.this.historyUriByUniqueURI.put(guid, uri);
							serviceInvoked.set(true);
							return UcmModel.getUniqueURI(attributes);
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

		UcmFSObject ret = this.objectByUniqueURI.get(guid);
		if (!UcmFile.class.isInstance(
			ret)) { throw new UcmFileRevisionNotFoundException(String.format("No revision found with ID [%s]", id)); }
		if (serviceInvoked.get()) {
			if (history.get() != null) {
				URI uri = ret.getURI();
				DataResultSet rs = history.get();
				LinkedList<UcmRevision> list = new LinkedList<>();
				for (DataObject o : rs.getRows()) {
					list.addFirst(new UcmRevision(uri, o, rs.getFields()));
				}
				this.historyByURI.put(uri, Tools.freezeList(new ArrayList<>(list)));
			}

			if (renditions.get() != null) {
				DataResultSet rs = renditions.get();
				Map<String, UcmRenditionInfo> m = new TreeMap<>();
				for (DataObject o : rs.getRows()) {
					UcmRenditionInfo r = new UcmRenditionInfo(guid, o, rs.getFields());
					m.put(r.getType().toUpperCase(), r);
				}
				this.renditionsByUniqueURI.put(guid, Tools.freezeMap(new LinkedHashMap<>(m)));
			}
		}
		return UcmFile.class.cast(ret);
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

	public static interface URIHandler {
		public void handleURI(UcmSession session, long pos, URI objectUri) throws UcmServiceException;
	}

	public Collection<URI> getURISearchResults(final UcmSession s, final String query) throws UcmServiceException {
		return getURISearchResults(s, query, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public Collection<URI> getURISearchResults(final UcmSession s, final String query, final int pageSize)
		throws UcmServiceException {
		final List<URI> results = new ArrayList<>();
		iterateURISearchResults(s, query, pageSize, new URIHandler() {
			@Override
			public void handleURI(UcmSession session, long pos, URI uri) {
				results.add(uri);
			}
		});
		return results;
	}

	public long iterateURISearchResults(final UcmSession s, final String query, final URIHandler handler)
		throws UcmServiceException {
		return iterateURISearchResults(s, query, UcmConstants.DEFAULT_PAGE_SIZE, handler);
	}

	public long iterateURISearchResults(final UcmSession s, final String query, int pageSize, final URIHandler handler)
		throws UcmServiceException {
		Objects.requireNonNull(s, "Must provide a session to search with");
		Objects.requireNonNull(query, "Must provide a query to execute");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");

		// Syntax: query{sortAtts}[(startRow,)?rowCount(/pageSize)?] / sortAtts == +att1,-att2,...
		Matcher m = UcmModel.QUERY_PARSER.matcher(StringUtils.strip(query));
		if (!m.matches()) { throw new UcmServiceException(String.format("Invalid query syntax: [%s]", query)); }

		final String actualQuery = m.group(1);
		if (StringUtils.isEmpty(
			actualQuery)) { throw new UcmServiceException("The actual query string is empty - this is not supported"); }

		final String sortField;
		final boolean desc;
		String sortSpecStr = m.group(2);
		if (sortSpecStr != null) {
			sortSpecStr = StringUtils.strip(sortSpecStr);
			Matcher sm = UcmModel.SORT_PARSER.matcher(sortSpecStr);
			if (!sm.matches()) {

			}

			sortField = sortSpecStr.replaceAll("^[-+]", "");
			desc = sortSpecStr.startsWith("-");
		} else {
			sortField = null;
			desc = false;
		}

		// TODO: This section supports multiple sort specifications - need to figure out why this
		// doesn't work...
		/*
		final StringBuilder sortSpec = new StringBuilder();
		if (sortSpecStr != null) {
			List<String> l = Tools.splitCSVEscaped(sortSpecStr);
			if (l.isEmpty()) { throw new UcmServiceException(String.format(
				"Illegal empty sort specification - the syntax is spec1[,spec2,spec3,...,specN] where specX is [-+]attributeName (+ = ASC, - = DESC, default is ASC)")); }
			for (String ss : l) {
				ss = StringUtils.strip(ss);
				Matcher sm = UcmModel.SORT_PARSER.matcher(ss);
				if (!sm.matches()) { throw new UcmServiceException(String.format(
					"Illegal attribute sort specification '%s' - the syntax is [-+]attributeName (+ = ASC, - = DESC, default is ASC)",
					ss)); }
				boolean desc = ss.startsWith("-");
				if (sortSpec.length() > 0) {
					sortSpec.append(", ");
				}
				if (ss.startsWith("-") || ss.startsWith("+")) {
					ss = ss.substring(1);
				}
				sortSpec.append(ss).append(' ').append(desc ? "De" : "A").append("sc");
			}
		}
		*/

		// TODO: Need to find the correct way to identify "automagically" if we should use database
		// or databasetext engines when running the search...if only there were documentation...
		final boolean dbMode = true;
		final AtomicLong startRow = new AtomicLong(1);
		final long maxRows;
		String rowSpec = m.group(3);
		if (rowSpec != null) {
			Matcher rm = UcmModel.ROW_PARSER.matcher(rowSpec);
			if (!rm.matches()) { throw new UcmServiceException(String.format(
				"Illegal row specification '%s' - the syntax is [startRow,]rowCount[/pageSize] where elements in [] are optional (numbers may not begin with a 0, and must be positive integers - the first row is 1)")); }
			String startRowStr = rm.group(1);
			if (startRowStr != null) {
				startRow.set(Long.parseLong(startRowStr));
			}
			maxRows = Integer.parseInt(rm.group(2));
			String pageSizeStr = rm.group(3);
			if (pageSizeStr != null) {
				pageSize = Integer.valueOf(pageSizeStr);
			}
		} else {
			maxRows = -1;
		}

		final int actualPageSize = Tools.ensureBetween(UcmConstants.MINIMUM_PAGE_SIZE, pageSize,
			UcmConstants.MAXIMUM_PAGE_SIZE);

		long rowNumber = 0;
		outer: while (true) {
			try {
				ServiceResponse response = s.callService("GET_SEARCH_RESULTS", new RequestPreparation() {
					@Override
					public void prepareRequest(DataBinder binder) {
						UcmModel.this.log.debug(
							"Calling GET_SEARCH_RESULTS (dbMode = {}, start row = {}, page size = {}, sortField = {} {}, query = [{}])",
							dbMode, startRow.get(), actualPageSize, sortField, desc ? "Desc" : "Asc", actualQuery);

						binder.putLocal("QueryText", actualQuery);
						if (dbMode) {
							binder.putLocal("SearchEngineName", "database");
						}
						binder.putLocal("StartRow", String.valueOf(startRow.get()));
						/*
						if (sortSpec.length() > 0) {
							binder.putLocal("SortSpec", sortSpec.toString());
						}
						*/
						if (sortField != null) {
							binder.putLocal("SortField", sortField);
							binder.putLocal("SortOrder", desc ? "Desc" : "Asc");
						}
						binder.putLocal("ResultCount", String.valueOf(actualPageSize));
						binder.putLocal("isAddFolderMetadata", "1");
					}
				});
				DataBinder binder = response.getResponseAsBinder();
				DataResultSet results = binder.getResultSet("SearchResults");
				if (results == null) {
					break;
				}
				List<DataObject> rows = results.getRows();
				if ((rows == null) || rows.isEmpty()) {
					break;
				}
				final boolean lastPage = (rows.size() < actualPageSize);
				for (DataObject o : results.getRows()) {
					try {
						URI uri = UcmModel.getURI(new UcmAttributes(o, results.getFields()));
						handler.handleURI(s, ++rowNumber, uri);
					} finally {
						startRow.incrementAndGet();
						if ((maxRows > 0) && (rowNumber >= maxRows)) {
							break outer;
						}
					}
				}
				if (lastPage) {
					break outer;
				}
			} catch (final IdcClientException e) {
				throw new UcmServiceException(String.format("Exception raised while performing the %s query [%s]",
					dbMode ? "database" : "fulltext", actualQuery), e);
			}
		}
		return rowNumber;
	}

	public static interface ObjectHandler {
		public void handleObject(UcmSession session, long pos, URI objectUri, UcmFSObject object);
	}

	public Collection<UcmFile> getDocumentSearchResults(final UcmSession s, final String query)
		throws UcmServiceException {
		return getDocumentSearchResults(s, query, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public Collection<UcmFile> getDocumentSearchResults(final UcmSession s, final String query, final int pageSize)
		throws UcmServiceException {
		final List<UcmFile> results = new ArrayList<>();
		iterateDocumentSearchResults(s, query, pageSize, new ObjectHandler() {
			@Override
			public void handleObject(UcmSession session, long pos, URI uri, UcmFSObject object) {
				if (UcmFile.class.isInstance(object)) {
					results.add(UcmFile.class.cast(object));
				}
			}
		});
		return results;
	}

	public long iterateDocumentSearchResults(final UcmSession s, final String query, final ObjectHandler handler)
		throws UcmServiceException {
		return iterateDocumentSearchResults(s, query, UcmConstants.DEFAULT_PAGE_SIZE, handler);
	}

	public long iterateDocumentSearchResults(final UcmSession s, final String query, int pageSize,
		final ObjectHandler handler) throws UcmServiceException {
		Objects.requireNonNull(s, "Must provide a session to search with");
		Objects.requireNonNull(query, "Must provide a query to execute");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");

		return iterateURISearchResults(s, query, new URIHandler() {
			@Override
			public void handleURI(UcmSession session, long pos, URI objectUri) throws UcmServiceException {
				final UcmFile file;
				try {
					file = getFile(s, objectUri);
				} catch (UcmFileNotFoundException e) {
					// The result was returned, but not accessible? KABOOM!
					throw new UcmServiceException(String.format(
						"The file with URI [%s] was returned in the result set, but was not retrieved when searched for explicitly",
						objectUri), e);
				}
				handler.handleObject(s, pos, file.getURI(), file);
			}
		});
	}

	public int iterateFolderContents(final UcmSession s, final UcmFolder folder, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(folder, "Must provide a folder object to iterate over");
		return iterateFolderContents(s, folder.getURI(), handler);
	}

	int iterateFolderContents(final UcmSession s, final URI uri, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(s, "Must provide a session to search with");
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
			Map<URI, UcmFSObject> objects = new LinkedHashMap<>(children.size());
			for (URI childUri : children.values()) {
				try {
					objects.put(childUri, getFSObject(s, childUri));
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
					handler.handleObject(s, ret++, childUri, objects.get(childUri));
				}
				return ret;
			}
		}

		final AtomicReference<UcmFSObject> data = new AtomicReference<>(null);
		final AtomicReference<Map<String, UcmFSObject>> rawChildren = new AtomicReference<>(null);
		if (children == null) {
			try {
				children = this.childrenByURI.createIfAbsent(uri, new ConcurrentInitializer<Map<String, URI>>() {
					@Override
					public Map<String, URI> get() throws ConcurrentException {
						try {
							try {
								Map<String, URI> children = new TreeMap<>();
								Map<String, UcmFSObject> dataObjects = new TreeMap<>();
								FolderContentsIterator it = new FolderContentsIterator(s, uri);
								while (it.hasNext()) {
									UcmAttributes o = it.next();
									URI childUri = UcmModel.getURI(o);
									String name = o.getString(UcmAtt.fFileName);
									if (name == null) {
										name = o.getString(UcmAtt.fFolderName);
									}
									children.put(name, childUri);
									dataObjects.put(name, newFSObject(childUri, o));
									handler.handleObject(s, it.getCurrentPos(), childUri, newFSObject(childUri, o));
								}
								rawChildren.set(dataObjects);
								UcmAttributes folderAtts = it.getFolder();
								URI folderUri = UcmModel.getURI(folderAtts);
								data.set(newFSObject(folderUri, folderAtts));
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
			Map<String, UcmFSObject> c = rawChildren.get();
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
			public void handleObject(UcmSession session, long pos, URI uri, UcmFSObject object) {
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
			public void handleObject(UcmSession session, long pos, URI uri, UcmFSObject data) {
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
				public void handleObject(UcmSession session, long pos, URI objectUri, UcmFSObject object) {
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
			public void handleObject(UcmSession session, long pos, URI uri, UcmFSObject obj) {
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
			public void handleObject(UcmSession session, long pos, URI uri, UcmFSObject o) {
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
		if (UcmModel.NULL_FOLDER_URI.equals(uri)) { return null; }
		try {
			UcmFSObject data = getFSObject(s, uri);
			if (UcmFolder.class.isInstance(data)) { return UcmFolder.class.cast(data); }
			throw new UcmFolderNotFoundException(String.format("The object with URI [%s] is not a folder", uri));
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
		final UcmFSObject file;
		try {
			file = getFSObject(s, uri);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}

		return getFileHistory(s, uri, file.getString(UcmAtt.dID));
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
				Tools.coalesce(rendition, UcmModel.RENDITION_DEFAULT_TYPE), file.getUniqueURI()), e);
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
					Tools.coalesce(rendition, UcmModel.RENDITION_DEFAULT_TYPE), file.getUniqueURI()));
			}
		}

		// Some other error!
		throw new UcmServiceException(String.format("Failed to load rendition [%s] from file [%s]",
			Tools.coalesce(rendition, UcmModel.RENDITION_DEFAULT_TYPE), file.getUniqueURI()));
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

	private UcmRenditionInfo generateDefaultRendition(UcmUniqueURI guid, UcmAttributes attributes) {
		String format = attributes.getString(UcmAtt.dFormat);
		if (format == null) {
			format = UcmModel.REMDITION_DEFAULT_FORMAT;
		}
		return new UcmRenditionInfo(guid, UcmModel.RENDITION_DEFAULT_TYPE, format, "Native File",
			"The original format (added automatically)");
	}

	public Map<String, UcmRenditionInfo> getRenditions(final UcmSession s, final UcmFile file)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		Objects.requireNonNull(file, "Must provide a file whose renditions to return");

		final UcmUniqueURI guid = file.getUniqueURI();
		Map<String, UcmRenditionInfo> renditions = this.renditionsByUniqueURI.get(guid);
		final AtomicReference<UcmFSObject> data = new AtomicReference<>(null);
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
											binder.putLocal("isAddFolderMetadata", "1");
										}
									});
									responseData = response.getResponseAsBinder();
								} catch (final IdcClientException e) {
									if (isNotFoundException(e, "Exception caught retrieving the revision with ID [%s]",
										id)) { throw new UcmFileRevisionNotFoundException(); }
									// This is a "regular" exception that we simply re-raise
									throw e;
								}

								UcmAttributes attributes = buildAttributesFromDocInfo(responseData, history, null);
								Map<String, UcmRenditionInfo> renditions = new TreeMap<>();
								DataResultSet rs = responseData.getResultSet("Renditions");
								if (rs != null) {
									for (DataObject o : rs.getRows()) {
										UcmRenditionInfo r = new UcmRenditionInfo(guid, o, rs.getFields());
										renditions.put(r.getType().toUpperCase(), r);
									}
								} else {
									UcmModel.this.log.warn(
										"Revision ID [{}] was found via DOC_INFO(dID={}), but no rendition information was returned??! Generated a default primary rendition",
										id, id);
									UcmRenditionInfo info = generateDefaultRendition(guid, attributes);
									renditions.put(info.getType(), info);
								}
								data.set(newFSObject(attributes));
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
			createIfAbsentInCache(this.objectByUniqueURI, guid, new ConcurrentInitializer<UcmFSObject>() {
				@Override
				public UcmFSObject get() throws ConcurrentException {
					UcmFSObject ret = data.get();
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