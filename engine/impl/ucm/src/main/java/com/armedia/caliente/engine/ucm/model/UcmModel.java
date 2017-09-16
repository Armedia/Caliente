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

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionFactory;
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

	private static final String FILE_SCHEME = "file";
	private static final String FOLDER_SCHEME = "folder";
	private static final String NULL_SCHEME = "null";

	private static final URI NULLURI = UcmModel.newURI(UcmModel.NULL_SCHEME, "null");
	private static final UcmGUID NULLGUID = new UcmGUID("<null>");

	// BY_GUID -> DataObject
	private final KeyLockableCache<UcmGUID, UcmAttributes> objectByGUID;

	// path -> file://${dDocName}
	// path -> folder://${fFolderGUID}
	// path -> NULLURI
	private final KeyLockableCache<String, URI> uriByPaths;

	// childURI -> parentURI
	private final KeyLockableCache<URI, URI> parentByURI;

	// parentURI -> Map<childName, childURI>
	private final KeyLockableCache<URI, Map<String, URI>> childrenByURI;

	// URI -> List<UcmGUID>
	private final KeyLockableCache<URI, List<UcmRevision>> historyByURI;

	// String -> UcmGUID
	private final KeyLockableCache<String, UcmGUID> versionGuidByID;

	// BY_GUID -> Map<String, UcmRenditionInfo>
	private final KeyLockableCache<UcmGUID, Map<String, UcmRenditionInfo>> renditionsByGUID;

	// URI -> BY_GUID
	private final KeyLockableCache<URI, UcmGUID> guidByURI;

	// BY_GUID -> URI
	private final KeyLockableCache<UcmGUID, URI> uriByGUID;

	private final UcmSessionFactory sessionFactory;

	private static boolean isFrameworkFoldersEnabled(UcmSessionFactory sessionFactory) throws UcmServiceException {
		final SessionWrapper<UcmSession> w;
		try {
			w = sessionFactory.acquireSession();
		} catch (Exception e) {
			throw new UcmServiceException("Failed to acquire an RIDC session", e);
		}
		try {
			UcmSession s = w.getWrapped();

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
		} finally {
			w.close();
		}
	}

	public UcmModel(UcmSessionFactory sessionFactory) throws UcmServiceException {
		Objects.requireNonNull(sessionFactory, "Must provide a non-null session factory");
		if (!UcmModel.isFrameworkFoldersEnabled(
			sessionFactory)) { throw new UcmServiceException("The FrameworkFolders component is not enabled"); }
		this.sessionFactory = sessionFactory;
		this.uriByPaths = new KeyLockableCache<>(1000);
		this.parentByURI = new KeyLockableCache<>(1000);
		this.childrenByURI = new KeyLockableCache<>(1000);
		this.historyByURI = new KeyLockableCache<>(1000);
		this.objectByGUID = new KeyLockableCache<>(1000);
		this.guidByURI = new KeyLockableCache<>(1000);
		this.uriByGUID = new KeyLockableCache<>(1000);
		this.renditionsByGUID = new KeyLockableCache<>(1000);
		this.versionGuidByID = new KeyLockableCache<>(1000);
	}

	protected final void cacheDataObject(DataObject object) {
		if (object == null) { return; }
		cacheDataObject(new UcmAttributes(object));
	}

	protected void cacheDataObject(final UcmAttributes data) {
		if (data == null) { return; }
		// Is this a file or a folder?
		final boolean file = data.hasAttribute(UcmAtt.dDocName);
		UcmAtt guidAtt = (file ? UcmAtt.fFileGUID : UcmAtt.fFolderGUID);
		final URI uri = UcmModel.getURI(data);
		final UcmGUID guid = new UcmGUID(data.getString(guidAtt));

		this.objectByGUID.put(guid, data);
		if (data.hasAttribute(UcmAtt.fParentGUID)) {
			this.parentByURI.put(uri, UcmModel.newFolderURI(data.getString(UcmAtt.fParentGUID)));
		}

		this.guidByURI.put(uri, guid);
		this.uriByGUID.put(guid, uri);
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
	public UcmFile getFile(String path) throws UcmServiceException, UcmFileNotFoundException {
		try {
			final URI uri = resolvePath(path);
			return new UcmFile(this, uri, getDataObject(uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}
	}

	protected URI resolvePath(String p) throws UcmServiceException, UcmObjectNotFoundException {
		final String sanitizedPath = UcmModel.sanitizePath(p);
		URI uri = this.uriByPaths.get(sanitizedPath);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		if (uri == null) {
			try {
				uri = this.uriByPaths.createIfAbsent(sanitizedPath, new ConcurrentInitializer<URI>() {
					@Override
					public URI get() throws ConcurrentException {
						final SessionWrapper<UcmSession> w;
						try {
							w = UcmModel.this.sessionFactory.acquireSession();
						} catch (Exception e) {
							throw new ConcurrentException("Failed to acquire an RIDC session", e);
						}
						try {
							UcmSession s = w.getWrapped();

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
						} finally {
							w.close();
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

	protected UcmAttributes getDataObject(final URI uri) throws UcmServiceException, UcmObjectNotFoundException {
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

		UcmGUID guid = this.guidByURI.get(uri);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> history = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> renditions = new AtomicReference<>(null);
		if (guid == null) {
			try {
				guid = this.guidByURI.createIfAbsent(uri, new ConcurrentInitializer<UcmGUID>() {
					@Override
					public UcmGUID get() throws ConcurrentException {
						final SessionWrapper<UcmSession> w;
						try {
							w = UcmModel.this.sessionFactory.acquireSession();
						} catch (Exception e) {
							throw new ConcurrentException("Failed to acquire an RIDC session", e);
						}
						try {
							UcmSession s = w.getWrapped();

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
									uri)) { return UcmModel.NULLGUID; }
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
							return new UcmGUID(baseData.getString(file ? UcmAtt.fFileGUID : UcmAtt.fFolderGUID));
						} catch (Exception e) {
							throw new ConcurrentException(e);
						} finally {
							w.close();
						}
					}
				});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				throw new UcmServiceException(String.format("Exception caught resolving URI [%s]", uri), cause);
			}
		}

		if (UcmModel.NULLGUID.equals(
			guid)) { throw new UcmObjectNotFoundException(String.format("No object found with URI [%s]", uri)); }

		UcmAttributes ret = createIfAbsentInCache(this.objectByGUID, guid, new ConcurrentInitializer<UcmAttributes>() {
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
			this.renditionsByGUID.put(guid, Tools.freezeMap(new LinkedHashMap<>(m)));
		}

		return ret;
	}

	public UcmFile getFileRevision(UcmRevision revision)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return new UcmFile(this, revision.getUri(), getFileRevision(revision.getId(), false, true));
	}

	public UcmFile getFileRevision(UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return new UcmFile(this, file.getURI(), getFileRevision(file.getRevisionId(), false, true));
	}

	protected UcmAttributes getFileRevision(final String id, boolean refreshHistory, final boolean refreshRenditions)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		UcmGUID guid = this.versionGuidByID.get(id);
		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> history = new AtomicReference<>(null);
		final AtomicReference<DataResultSet> renditions = new AtomicReference<>(null);
		if (guid == null) {
			try {
				guid = this.versionGuidByID.createIfAbsent(id, new ConcurrentInitializer<UcmGUID>() {
					@Override
					public UcmGUID get() throws ConcurrentException {
						final SessionWrapper<UcmSession> w;
						try {
							w = UcmModel.this.sessionFactory.acquireSession();
						} catch (Exception e) {
							throw new ConcurrentException("Failed to acquire an RIDC session", e);
						}
						try {
							UcmSession s = w.getWrapped();

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
									id)) { return UcmModel.NULLGUID; }
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
							renditions.set(responseData.getResultSet("Renditions"));

							UcmAttributes baseData = new UcmAttributes(baseObj);
							data.set(baseData);
							return new UcmGUID(baseData.getString(UcmAtt.fFileGUID));
						} catch (Exception e) {
							throw new ConcurrentException(e);
						} finally {
							w.close();
						}
					}
				});
			} catch (ConcurrentException e) {
				Throwable cause = e.getCause();
				UcmModel.throwIfMatches(UcmServiceException.class, cause);
				throw new UcmServiceException(String.format("Exception caught locating revision ID [%s]", id), cause);
			}
		}

		if (UcmModel.NULLGUID.equals(
			guid)) { throw new UcmFileRevisionNotFoundException(String.format("No revision found with ID [%s]", id)); }

		UcmAttributes ret = createIfAbsentInCache(this.objectByGUID, guid, new ConcurrentInitializer<UcmAttributes>() {
			@Override
			public UcmAttributes get() throws ConcurrentException {
				UcmAttributes ret = data.get();
				cacheDataObject(ret);
				return ret;
			}
		});

		URI uri = UcmModel.getURI(ret);

		if (refreshHistory && (history.get() != null)) {
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
			this.renditionsByGUID.put(guid, Tools.freezeMap(new LinkedHashMap<>(m)));
		}

		return ret;
	}

	protected Map<String, URI> getChildren(String path) throws UcmServiceException, UcmFolderNotFoundException {
		try {
			return getFolderContents(resolvePath(path));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	protected Map<String, URI> getFolderContents(final UcmGUID guid)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(guid, "Must provide a BY_GUID to search for");
		// If it's a folder URI we already know the SSP is the BY_GUID, so... go!
		return getFolderContents(UcmModel.newFolderURI(guid.getString()));
	}

	public static interface ObjectHandler {
		public void handleObject(UcmSession session, int pos, URI objectUri, UcmAttributes object);
	}

	public int iterateFolderContents(final URI uri, final ObjectHandler handler)
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
					objects.put(childUri, getDataObject(childUri));
				} catch (UcmObjectNotFoundException e) {
					reconstruct = true;
					break;
				}
			}

			// If the cache remains current, and we're not missing any child objects, then we quite
			// simply iterate over the objects we got and call it a day
			if (!reconstruct) {
				final SessionWrapper<UcmSession> w;
				try {
					w = UcmModel.this.sessionFactory.acquireSession();
				} catch (Exception e) {
					throw new UcmServiceException("Failed to acquire an RIDC session", e);
				}
				try {
					final UcmSession session = w.getWrapped();
					int ret = 0;
					for (URI childUri : objects.keySet()) {
						handler.handleObject(session, ret++, childUri, objects.get(childUri));
					}
					return ret;
				} finally {
					w.close();
				}
			}
		}

		final AtomicReference<UcmAttributes> data = new AtomicReference<>(null);
		final AtomicReference<Map<String, UcmAttributes>> rawChildren = new AtomicReference<>(null);
		if (children == null) {
			try {
				children = this.childrenByURI.createIfAbsent(uri, new ConcurrentInitializer<Map<String, URI>>() {
					@Override
					public Map<String, URI> get() throws ConcurrentException {
						final SessionWrapper<UcmSession> w;
						try {
							w = UcmModel.this.sessionFactory.acquireSession();
						} catch (Exception e) {
							throw new ConcurrentException("Failed to acquire an RIDC session", e);
						}
						try {
							UcmSession s = w.getWrapped();
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
						} finally {
							w.close();
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

	protected Map<String, URI> getFolderContents(final URI uri) throws UcmServiceException, UcmFolderNotFoundException {
		final Map<String, URI> children = new LinkedHashMap<>();
		iterateFolderContents(uri, new ObjectHandler() {
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

	public Map<String, UcmFSObject> getFolderContents(final UcmFolder folder)
		throws UcmServiceException, UcmFolderNotFoundException {
		final Map<String, UcmFSObject> children = new LinkedHashMap<>();
		iterateFolderContents(folder.getURI(), new ObjectHandler() {
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

	public UcmFolder getFolder(String path) throws UcmServiceException, UcmFolderNotFoundException {
		try {
			final URI uri = resolvePath(path);
			return new UcmFolder(this, uri, getDataObject(uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	UcmFolder getFolder(UcmGUID guid) throws UcmServiceException, UcmFolderNotFoundException {
		try {
			final URI uri = UcmModel.newFolderURI(guid.getString());
			return new UcmFolder(this, uri, getDataObject(uri));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFileHistory getFileHistoryByPath(String path)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileHistory(getFile(path));
	}

	public UcmFileHistory getFileHistory(final UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileHistory(file.getURI(), file.getRevisionId());
	}

	public UcmFileHistory getFileHistory(final URI uri)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		final UcmAttributes att;
		try {
			att = getDataObject(uri);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}

		return getFileHistory(uri, att.getString(UcmAtt.dID));
	}

	UcmFileHistory getFileHistory(final URI uri, final String revisionId)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		List<UcmRevision> history = this.historyByURI.get(uri);
		if (history == null) {
			try {
				history = this.historyByURI.createIfAbsent(uri, new ConcurrentInitializer<List<UcmRevision>>() {
					@Override
					public List<UcmRevision> get() throws ConcurrentException {
						final SessionWrapper<UcmSession> w;
						try {
							w = UcmModel.this.sessionFactory.acquireSession();
						} catch (Exception e) {
							throw new ConcurrentException("Failed to acquire an RIDC session", e);
						}
						try {
							UcmSession s = w.getWrapped();

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
						} finally {
							w.close();
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

	InputStream getInputStream(UcmFile file, String rendition)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		final SessionWrapper<UcmSession> w;
		try {
			w = UcmModel.this.sessionFactory.acquireSession();
		} catch (Exception e) {
			throw new UcmServiceException("Failed to acquire an RIDC session", e);
		}
		try {
			UcmSession s = w.getWrapped();

			DataBinder binder = s.createBinder();
			binder.putLocal("IdcService", "GET_FILE");
			binder.putLocal("dID", String.valueOf(file.getRevisionId()));

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
		} finally {
			w.close();
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

	UcmFile refresh(UcmFile f) throws UcmFileNotFoundException, UcmServiceException, UcmFileRevisionNotFoundException {
		return getFileRevision(f);
	}

	UcmFolder refresh(UcmFolder f) throws UcmFolderNotFoundException, UcmServiceException {
		return getFolder(f.getObjectGUID());
	}

	UcmFileHistory refresh(UcmFileHistory h)
		throws UcmFileNotFoundException, UcmServiceException, UcmFileRevisionNotFoundException {
		return getFileHistory(h.getURI());
	}
}