package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.caliente.engine.ucm.UcmSessionFactory;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;

@SuppressWarnings("unused")
public class UcmModel {
	private static final Pattern PATH_CHECKER = Pattern.compile("^(/|(/[^/]+)+/?)$");

	private static final URI NULLURI = UcmModel.newURI("null", "null");

	// GUID -> DataObject
	private final KeyLockableCache<UcmGUID, DataObject> objectByGUID;

	// path -> file://${dDocName}
	// path -> folder://${fFolderGUID}
	// path -> NULLURI
	private final KeyLockableCache<String, URI> uriByPaths;

	// childURI -> parentURI
	private final KeyLockableCache<URI, URI> parentByURI;

	// parentURI -> Map<childName, childURI>
	private final KeyLockableCache<URI, Map<String, URI>> childrenByURI;

	// URI -> List<UcmGUID>
	private final KeyLockableCache<URI, List<UcmGUID>> historyByContentID;

	// GUID -> URI
	private final KeyLockableCache<UcmGUID, URI> uriByGUID;

	// These are so we don't construct the same objects over and over again...
	private final KeyLockableCache<URI, UcmFileHistory> historyInstances;
	private final KeyLockableCache<UcmGUID, UcmFile> fileInstances;
	private final KeyLockableCache<UcmGUID, UcmFolder> folderInstances;

	private final UcmSessionFactory sessionFactory;

	public UcmModel(UcmSessionFactory sessionFactory) {
		Objects.requireNonNull(sessionFactory, "Must provide a non-null session factory");
		this.sessionFactory = sessionFactory;
		this.uriByPaths = new KeyLockableCache<>(1000);
		this.parentByURI = new KeyLockableCache<>(1000);
		this.childrenByURI = new KeyLockableCache<>(1000);
		this.historyByContentID = new KeyLockableCache<>(1000);
		this.objectByGUID = new KeyLockableCache<>(1000);
		this.uriByGUID = new KeyLockableCache<>(1000);
		this.historyInstances = new KeyLockableCache<>(1000);
		this.fileInstances = new KeyLockableCache<>(1000);
		this.folderInstances = new KeyLockableCache<>(1000);
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
		URI uri = resolvePath(path);
		DataObject object = getDataObject(uri);
		// TODO: should we try to find a cached UcmFile instance instead, and avoid wild
		// construction?
		return new UcmFile(this, object);
	}

	protected URI resolvePath(String p) throws UcmServiceException, UcmFileNotFoundException {
		final String sanitizedPath = UcmModel.sanitizePath(p);
		URI uri = this.uriByPaths.get(sanitizedPath);
		final AtomicReference<UcmTools> data = new AtomicReference<>(null);
		if (uri == null) {
			try {
				uri = this.uriByPaths.createIfAbsent(sanitizedPath, new ConcurrentInitializer<URI>() {
					@Override
					public URI get() throws ConcurrentException {
						try {
							SessionWrapper<IdcSession> w = UcmModel.this.sessionFactory.acquireSession();
							IdcSession s = w.getWrapped();

							DataBinder binder = s.createBinder();
							binder.putLocal("IdcService", "FLD_INFO");
							binder.putLocal("path", sanitizedPath);

							ServiceResponse response = null;
							DataBinder responseData = null;
							try {
								response = s.sendRequest(binder);
								responseData = response.getResponseAsBinder();
							} catch (final IdcClientException e) {
								// Is this a service exception from which we can identify that the
								// item doesn't exist?
								if (!ServiceException.class.isInstance(e)) {
									// No, this isn't an exception we can analyze...
									throw new UcmServiceException(
										String.format("Exception caught retrieving the file at [%s]", sanitizedPath),
										e);
								}

								// This may be an analyzable exception
								ServiceException se = ServiceException.class.cast(e);
								String mk = se.getBinder().getLocal("StatusMessageKey");
								if (mk.startsWith("!csFldDoesNotExist,")) {
									// This is an exception telling us the item doesn't exist!!
									return UcmModel.NULLURI;
								}

								// This is a "regular" exception that we simply re-raise
								throw e;
							}

							// First things first!! Stash the retrieved object...
							DataResultSet rs = responseData.getResultSet("FileInfo");
							if (rs == null) {
								rs = responseData.getResultSet("FolderInfo");
							}
							if (rs == null) { throw new UcmServiceException(String.format(
								"Path [%s] was found, but was neither a file nor a folder?!?", sanitizedPath)); }
							data.set(new UcmTools(rs.getRows().get(0)));

							String guid = data.get().getString(UcmAtt.dDocName);
							if (guid != null) { return UcmModel.newURI("file", guid, null); }

							guid = data.get().getString(UcmAtt.fFolderGUID);
							if (guid != null) { return UcmModel.newURI("folder", guid, null); }

							throw new UcmServiceException(String.format(
								"Path [%s] was found, but was neither a file nor a folder (no identifier attributes)?!?",
								sanitizedPath));
						} catch (IdcClientException | UcmServiceException e) {
							throw new ConcurrentException(e);
						} catch (Exception e) {
							// Session issue?
							e.printStackTrace();
							return UcmModel.NULLURI;
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

		if (Tools.equals(UcmModel.NULLURI,
			uri)) { throw new UcmFileNotFoundException(String.format("No object found at path [%s]", sanitizedPath)); }
		return uri;
	}

	protected final void cacheDataObject(DataObject object) {
		if (object == null) { return; }
		cacheDataObject(new UcmTools(object));
	}

	protected void cacheDataObject(UcmTools data) {
		if (data == null) { return; }
		// Is this a file or a folder?
		final boolean file = data.hasAttribute(UcmAtt.dDocName);
		UcmAtt guidAtt = (file ? UcmAtt.fFileGUID : UcmAtt.fFolderGUID);
		final UcmGUID guid = new UcmGUID(data.getString(guidAtt));
		final URI uri = (file ? UcmModel.newURI("file", data.getString(UcmAtt.dDocName))
			: UcmModel.newURI("folder", data.getString(UcmAtt.fFolderGUID)));

		this.objectByGUID.put(guid, data.getDataObject());
		if (data.hasAttribute(UcmAtt.fParentGUID)) {
			this.parentByURI.put(uri, UcmModel.newURI("folder", data.getString(UcmAtt.fParentGUID), null));
		}
		this.uriByGUID.put(guid, uri);
	}

	protected static final URI getURI(DataObject dataObject) {
		return UcmModel.getURI(new UcmTools(dataObject));
	}

	protected static final URI getURI(UcmTools data) {
		final boolean file = data.hasAttribute(UcmAtt.dDocName);
		return (file ? UcmModel.newURI("file", data.getString(UcmAtt.dDocName))
			: UcmModel.newURI("folder", data.getString(UcmAtt.fFolderGUID)));
	}

	protected static final URI newURI(String scheme, String ssp) {
		return UcmModel.newURI(scheme, ssp, null);
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

	protected DataObject getDataObject(URI uri) throws UcmServiceException {
		// Here we determine if the URI is for a file or a folder, and retrieve stuff accordingly...
		return null;
	}

	/**
	 * Returns the given revision of the file at the given path, or {@code null} if none exists.
	 *
	 * @param path
	 *            The absolute path to the file. It will be normalized (i.e. "." and ".." will be
	 *            resolved).
	 * @return the file at the given path, or {@code null} if none exists.
	 * @throws UcmException
	 */
	public UcmFile getFile(String path, int revision) throws UcmException {
		UcmFile file = getFile(path);

		return null;
	}

	UcmFile getFile(UcmGUID guid) throws UcmException {
		return null;
	}

	public UcmFolder getFolder(String path) throws UcmException {
		return null;
	}

	public UcmFileHistory getFileHistory(String path) throws UcmException {
		return null;
	}

	public UcmFileHistory getFileHistory(UcmFile file) throws UcmException {
		return null;
	}

	Iterator<UcmFSObject> getFolderContents(UcmFolder folder) throws UcmException {
		return null;
	}

	UcmFolder getFolder(UcmGUID guid) throws UcmException {
		return null;
	}

	InputStream getInputStream(UcmFile file, String rendition) throws UcmException {
		return null;
	}

	boolean isStale(UcmModelObject obj) {
		return false;
	}

	void refresh(UcmFile file) throws UcmException {
		if (!isStale(file)) { return; }
	}

	void refresh(UcmFolder folder) throws UcmException {
		if (!isStale(folder)) { return; }
	}

	void refresh(UcmFileHistory history) throws UcmException {
		if (!isStale(history)) { return; }
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
}