package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;

public class UcmModel {
	private static final Pattern PATH_CHECKER = Pattern.compile("^(/|(/[^/]+)+/?)$");
	private static final int MIN_OBJECT_COUNT = 100;
	private static final int DEFAULT_OBJECT_COUNT = 10000;
	private static final int MAX_OBJECT_COUNT = 1000000;

	private static final String FILE_SCHEME = "file";
	private static final String FOLDER_SCHEME = "folder";
	private static final String NULL_SCHEME = "null";

	private static final URI NULLURI = UcmModel.newURI(UcmModel.NULL_SCHEME, "null");

	// Unique URI -> DataObject
	private final KeyLockableCache<UcmUniqueURI, UcmAttributes> objectByUniqueURI;

	// path -> URI
	private final KeyLockableCache<String, URI> uriByPaths;

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
			DataBinder binder = s.createBinder();
			binder.putLocal("IdcService", "CONFIG_INFO");

			ServiceResponse response = null;
			DataBinder responseData = null;
			try {
				response = s.sendRequest(binder);
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
		this.parentByURI = new KeyLockableCache<>(objectCount);
		this.childrenByURI = new KeyLockableCache<>(objectCount);
		this.historyByURI = new KeyLockableCache<>(objectCount);
		this.objectByUniqueURI = new KeyLockableCache<>(objectCount);
		this.uniqueUriByHistoryUri = new KeyLockableCache<>(objectCount);
		this.historyUriByUniqueURI = new KeyLockableCache<>(objectCount);
		this.renditionsByUniqueURI = new KeyLockableCache<>(objectCount);
		this.revisionUriByRevisionID = new KeyLockableCache<>(objectCount);
	}

	protected final void cacheDataObject(DataObject object) {
		if (object == null) { return; }
		cacheDataObject(new UcmAttributes(object));
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

	protected static final URI getURI(DataObject dataObject) {
		return UcmModel.getURI(new UcmAttributes(dataObject));
	}

	protected static final URI getURI(UcmAttributes data) {
		final boolean file = data.hasAttribute(UcmAtt.dDocName);
		return (file ? UcmModel.newFileURI(data.getString(UcmAtt.dDocName))
			: UcmModel.newFolderURI(data.getString(UcmAtt.fFolderGUID)));
	}

	protected static final UcmUniqueURI getUniqueURI(UcmAttributes data) {
		final boolean file = data.hasAttribute(UcmAtt.dDocName);
		URI uri = UcmModel.getURI(data);
		if (file) {
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
		String mk = se.getBinder().getLocal("StatusMessageKey");
		for (List<String> l : UcmExceptionData.parseMessages(mk)) {
			if (!l.isEmpty()) {
				String op = l.get(0);
				if (Tools.equals(op, "csFldDoesNotExist") || //
					Tools.equals(op, "csUnableToGetRevInfo2") || //
					Tools.equals(op, "csGetFileUnableToFindRevision")) {
					// TODO: Maybe we have to index more error labels here?
					return true;
				}
			}
		}
		return false;
	}

	public final boolean isFileURI(URI uri) {
		return UcmModel.FILE_SCHEME.equals(uri.getScheme());
	}

	public final boolean isFolderURI(URI uri) {
		return UcmModel.FOLDER_SCHEME.equals(uri.getScheme());
	}

	/**
	 * Returns the latest revision of the file at the given path.
	 *
	 * @param path
	 *            The absolute path to the file. It will be normalized (i.e. "." and ".." will be
	 *            resolved).
	 * @return the file at the given path
	 * @throws UcmServiceException
	 *             if there's a communications problem
	 * @throws UcmFileNotFoundException
	 *             if there is no file at that path
	 */
	public UcmFile getFile(UcmSession s, String path) throws UcmServiceException, UcmFileNotFoundException {
		try {
			final URI uri = resolvePath(s, path);
			return new UcmFile(this, uri, getDataObject(s, uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}
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
							DataBinder binder = s.createBinder();
							binder.putLocal("IdcService", "FLD_INFO");
							binder.putLocal("path", sanitizedPath);

							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.sendRequest(binder);
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the file at [%s]",
									sanitizedPath)) { return UcmModel.NULLURI; }
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
							baseObj.put(UcmAtt.$ucmParentPath.name(), FileNameTools.dirname(parentPath, '/'));
							data.set(new UcmAttributes(baseObj));

							String guid = data.get().getString(UcmAtt.dDocName);
							if (guid != null) { return UcmModel.newFileURI(guid); }

							guid = data.get().getString(UcmAtt.fFolderGUID);
							if (guid != null) { return UcmModel.newFolderURI(guid); }

							throw new UcmServiceException(String.format(
								"Path [%s] was found, but was neither a file nor a folder (no identifier attributes)?!?",
								sanitizedPath));
						} catch (Exception e) {
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

		if (Tools.equals(UcmModel.NULLURI, uri)) { throw new UcmObjectNotFoundException(
			String.format("No object found at path [%s]", sanitizedPath)); }
		return uri;
	}

	protected UcmAttributes getDataObject(final UcmSession s, final URI uri)
		throws UcmServiceException, UcmObjectNotFoundException {
		final boolean file;
		if (isFileURI(uri)) {
			// The SSP is the dDocName
			file = true;
		} else if (isFolderURI(uri)) {
			// The SSP is the BY_GUID
			file = false;
		} else {
			// WTF?? Invalid URI
			throw new IllegalArgumentException(String.format("The URI [%s] doesn't point to a valid object", uri));
		}

		UcmUniqueURI guid = this.uniqueUriByHistoryUri.get(uri);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> history = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> renditions = new AtomicReference<>(null);
		if (guid == null) {
			try {
				guid = this.uniqueUriByHistoryUri.createIfAbsent(uri, new ConcurrentInitializer<UcmUniqueURI>() {
					@Override
					public UcmUniqueURI get() throws ConcurrentException {
						try {
							DataBinder binder = s.createBinder();
							if (file) {
								binder.putLocal("IdcService", "DOC_INFO_BY_NAME");
								binder.putLocal("dDocName", uri.getSchemeSpecificPart());
								binder.putLocal("RevisionSelectionMethod", "Latest");
								binder.putLocal("includeFileRenditionsInfo", "1");
							} else {
								binder.putLocal("IdcService", "FLD_INFO");
								binder.putLocal("fFolderGUID", uri.getSchemeSpecificPart());
							}

							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.sendRequest(binder);
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								if (isNotFoundException(e, "Exception caught retrieving the URI [%s]",
									uri)) { return UcmUniqueURI.NULL_GUID; }
								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							// First things first!! Stash the retrieved object...
							final String prefix = (file ? "File" : "Folder");
							DataResultSet rs = responseData.getResultSet(String.format("%sInfo", prefix));
							if (rs == null) { throw new UcmServiceException(
								String.format("URI [%s] was found, but returned incorrect results?!?", uri)); }

							Map<String, String> baseObj = new HashMap<>();
							baseObj.putAll(rs.getRows().get(0));
							if (file) {
								DataObject docInfo = responseData.getResultSet("DOC_INFO").getRows().get(0);
								baseObj.putAll(docInfo);
								history.set(responseData.getResultSet("REVISION_HISTORY"));
								renditions.set(responseData.getResultSet("Renditions"));
								// Capture the parent path...from DOC_INFO_BY_NAME, it's stored in
								// LocalData.fParentPath
								baseObj.put(UcmAtt.$ucmParentPath.name(),
									responseData.getLocalData().get("fParentPath"));
							} else {
								// Capture the parent path...from FLD_INFO, it's stored in
								// LocalData.folderPath, but it also includes the folder's name, so
								// we dirname it
								String path = responseData.getLocalData().get("folderPath");
								baseObj.put(UcmAtt.$ucmParentPath.name(), FileNameTools.dirname(path, '/'));
							}

							UcmAttributes baseData = new UcmAttributes(baseObj);
							data.set(baseData);
							return UcmModel.getUniqueURI(baseData);
						} catch (Exception e) {
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

		UcmAttributes ret = createIfAbsentInCache(this.objectByUniqueURI, guid, new ConcurrentInitializer<UcmAttributes>() {
			@Override
			public UcmAttributes get() throws ConcurrentException {
				UcmAttributes ret = data.get();
				cacheDataObject(ret);
				return ret;
			}
		});

		if (history.get() != null) {
			DataResultSet rs = history.get();
			LinkedList<UcmRevision> list = new LinkedList<>();
			for (DataObject o : rs.getRows()) {
				list.addFirst(new UcmRevision(o));
			}
			this.historyByURI.put(uri, Tools.freezeList(new ArrayList<>(list)));
		}

		if (renditions.get() != null) {
			DataResultSet rs = history.get();
			Map<String, UcmRenditionInfo> m = new TreeMap<>();
			for (DataObject o : rs.getRows()) {
				UcmRenditionInfo r = new UcmRenditionInfo(guid, o);
				m.put(r.getName(), r);
			}
			this.renditionsByUniqueURI.put(guid, Tools.freezeMap(new LinkedHashMap<>(m)));
		}

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
							DataBinder binder = s.createBinder();
							binder.putLocal("IdcService", "DOC_INFO");
							binder.putLocal("dID", id);
							if (refreshRenditions) {
								binder.putLocal("includeFileRenditionsInfo", "1");
							}

							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.sendRequest(binder);
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
							baseObj.put(UcmAtt.$ucmParentPath.name(), responseData.getLocalData().get("fParentPath"));

							DataObject docInfo = responseData.getResultSet("DOC_INFO").getRows().get(0);
							baseObj.putAll(docInfo);
							history.set(responseData.getResultSet("REVISION_HISTORY"));
							if (refreshRenditions) {
								renditions.set(responseData.getResultSet("Renditions"));
							}

							UcmAttributes baseData = new UcmAttributes(baseObj);
							data.set(baseData);
							return UcmModel.getUniqueURI(baseData);
						} catch (Exception e) {
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

		UcmAttributes ret = createIfAbsentInCache(this.objectByUniqueURI, guid, new ConcurrentInitializer<UcmAttributes>() {
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
				list.addFirst(new UcmRevision(o));
			}
			this.historyByURI.put(uri, Tools.freezeList(new ArrayList<>(list)));
		}

		if (refreshRenditions && (renditions.get() != null)) {
			DataResultSet rs = renditions.get();
			Map<String, UcmRenditionInfo> m = new TreeMap<>();
			for (DataObject o : rs.getRows()) {
				UcmRenditionInfo r = new UcmRenditionInfo(guid, o);
				m.put(r.getName(), r);
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
		public void handleObject(UcmSession session, int pos, URI objectUri, UcmAttributes object);
	}

	public int iterateFolderContents(final UcmSession s, final URI uri, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to search for");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");
		// If this isn't a folder, we don't even try it...
		if (!isFolderURI(uri)) { return -1; }

		Map<String, URI> children = this.childrenByURI.get(uri);
		boolean reconstruct = false;
		if (children != null) {
			Map<URI, UcmAttributes> objects = new LinkedHashMap<>(children.size());
			// We'll gather the objects first, and then iterate over them, because
			// if there's an inconsistency (i.e. a missing stale object), then we
			// want to do the full service invocation to the server
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
					handler.handleObject(s, ret++, childUri, objects.get(childUri));
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
									// Here we check the handler's state to see if we should invoke
									// handleObject(), but we don't break the cycle just yet because
									// we want to cache everything we retrieved...
									handler.handleObject(s, it.getCurrentPos(), childUri, o);
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
						} catch (Exception e) {
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
			public void handleObject(UcmSession session, int pos, URI uri, UcmAttributes data) {
				String name = data.getString(UcmAtt.fFileName);
				if (name == null) {
					name = data.getString(UcmAtt.fFolderName);
				}
				children.put(name, uri);
			}
		});
		return children;
	}

	public Map<String, UcmFSObject> getFolderContents(UcmSession s, final UcmFolder folder)
		throws UcmServiceException, UcmFolderNotFoundException {
		final Map<String, UcmFSObject> children = new LinkedHashMap<>();
		iterateFolderContents(s, folder.getURI(), new ObjectHandler() {
			@Override
			public void handleObject(UcmSession session, int pos, URI uri, UcmAttributes data) {
				UcmFSObject o = null;
				String name = data.getString(UcmAtt.fFileName);
				if (name != null) {
					o = new UcmFile(UcmModel.this, uri, data);
				} else {
					name = data.getString(UcmAtt.fFolderName);
					o = new UcmFolder(UcmModel.this, uri, data);
				}
				children.put(name, o);
			}
		});
		return children;
	}

	public UcmFolder getFolder(UcmSession s, String path) throws UcmServiceException, UcmFolderNotFoundException {
		try {
			final URI uri = resolvePath(s, path);
			return new UcmFolder(this, uri, getDataObject(s, uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	UcmFolder getFolder(UcmSession s, UcmUniqueURI uri) throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a unique URI to locate");
		return getFolder(s, uri.getURI());
	}

	UcmFolder getFolder(UcmSession s, URI uri) throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to locate");
		try {
			return new UcmFolder(this, uri, getDataObject(s, uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFileHistory getFileHistoryByPath(UcmSession s, String path)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileHistory(s, getFile(s, path));
	}

	public UcmFileHistory getFileHistory(UcmSession s, UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileHistory(s, file.getURI(), file.getRevisionId());
	}

	public UcmFileHistory getFileHistory(UcmSession s, URI uri)
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
							DataBinder binder = s.createBinder();
							binder.putLocal("IdcService", "REV_HISTORY");
							binder.putLocal("dID", String.valueOf(revisionId));

							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.sendRequest(binder);
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
								info.addFirst(new UcmRevision(o));
							}
							return info;
						} catch (Exception e) {
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

	InputStream getInputStream(UcmSession s, UcmFile file, String rendition)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		try {
			DataBinder binder = s.createBinder();
			binder.putLocal("IdcService", "GET_FILE");
			binder.putLocal("dID", String.valueOf(file.getRevisionId()));
			if (!StringUtils.isEmpty(rendition)) {
				binder.putLocal("Rendition", rendition);
			}

			try {
				return s.sendRequest(binder).getResponseStream();
			} catch (final IdcClientException e) {
				if (isNotFoundException(e, "Exception caught retrieving the URI [%s]",
					file.getURI())) { throw new UcmFileNotFoundException(
						String.format("No file found with URI [%s]", file.getURI())); }
				// This is a "regular" exception that we simply re-raise
				throw e;
			}
		} catch (Exception e) {
			throw new UcmServiceException(e.getMessage(), e);
		}
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
								DataBinder binder = s.createBinder();
								binder.putLocal("IdcService", "DOC_INFO");
								binder.putLocal("dID", id);
								binder.putLocal("includeFileRenditionsInfo", "1");

								ServiceResponse response = null;
								DataBinder responseData = null;
								try {
									response = s.sendRequest(binder);
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
								baseObj.put(UcmAtt.$ucmParentPath.name(),
									responseData.getLocalData().get("fParentPath"));

								DataObject docInfo = responseData.getResultSet("DOC_INFO").getRows().get(0);
								baseObj.putAll(docInfo);
								history.set(responseData.getResultSet("REVISION_HISTORY"));

								Map<String, UcmRenditionInfo> renditions = new TreeMap<>();
								rs = responseData.getResultSet("Renditions");
								if (rs == null) { throw new UcmServiceException(String.format(
									"Revision ID [%s] was found, but no rendition information was returned??!", id)); }
								for (DataObject o : rs.getRows()) {
									UcmRenditionInfo r = new UcmRenditionInfo(guid, o);
									renditions.put(r.getName(), r);
								}

								UcmAttributes baseData = new UcmAttributes(baseObj);
								data.set(baseData);
								return renditions;
							} catch (Exception e) {
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
				list.addFirst(new UcmRevision(o));
			}
			this.historyByURI.put(file.getURI(), Tools.freezeList(new ArrayList<>(list)));
		}
		return new TreeMap<>(renditions);
	}
}