package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.caliente.engine.ucm.UcmSessionFactory;
import com.armedia.commons.utilities.LockDispenser;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;

@SuppressWarnings("unused")
public class UcmModel {
	private static final Pattern PATH_CHECKER = Pattern.compile("^(/|(/[^/]+)+/?)$");

	private static final URI NULLURI;
	static {
		try {
			NULLURI = new URI("null://null");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Unexpected URI syntax exception", e);
		}
	}

	private static class Locker<K> extends LockDispenser<K, ReadWriteLock> {
		@Override
		protected ReadWriteLock newLock(K key) {
			return new ReentrantReadWriteLock();
		}
	}

	// UcmGUID -> DataObject
	private CacheAccess<UcmGUID, DataObject> objectsByGUID = JCS.getInstance("ObjectsByGuid");
	private Locker<UcmGUID> objectsByGUIDLocks = new Locker<>();

	// path -> file://${dDocName}
	// path -> folder://${fFolderGUID}
	// path -> NULLURI
	private final Map<String, URI> uriByPaths;
	private Locker<String> uriByPathsLocks = new Locker<>();

	// childURI -> parentURI
	private final Map<URI, URI> parentsByURI;
	private Locker<URI> parentsByURILocks = new Locker<>();

	// parentURI -> childURI
	private final Map<URI, URI> childrenByURI;
	private Locker<URI> childrenByURILocks = new Locker<>();

	// URI -> List<UcmGUID>
	private Map<URI, List<UcmGUID>> historiesByContentID;
	private Locker<URI> historiesByContentIDLocks = new Locker<>();

	// GUID -> DataObject
	private final Map<UcmGUID, DataObject> objects;
	private final Map<UcmGUID, URI> guidToURI;

	// These are so we don't construct the same objects over and over again...
	private final Map<URI, UcmFileHistory> historyInstances;
	private final Map<UcmGUID, UcmFile> fileInstances;
	private final Map<UcmGUID, UcmFolder> folderInstances;

	private final UcmSessionFactory sessionFactory;

	public UcmModel(UcmSessionFactory sessionFactory) {
		Objects.requireNonNull(sessionFactory, "Must provide a non-null session factory");
		this.sessionFactory = sessionFactory;

		this.uriByPaths = new LRUMap<>(1000);
		this.parentsByURI = new LRUMap<>(1000);
		this.childrenByURI = new LRUMap<>(1000);
		this.historiesByContentID = new LRUMap<>(1000);
		this.objects = new LRUMap<>(1000);
		this.guidToURI = new LRUMap<>(1000);
		this.historyInstances = new LRUMap<>(1000);
		this.fileInstances = new LRUMap<>(1000);
		this.folderInstances = new LRUMap<>(1000);
	}

	public UcmFile getFile(String path) throws UcmServiceException, UcmFileNotFoundException {
		return getFile(path, false);
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
	public UcmFile getFile(String path, boolean forced) throws UcmServiceException, UcmFileNotFoundException {
		URI uri = resolvePath(path, forced);
		DataObject object = getDataObject(uri);
		// TODO: should we try to find a cached UcmFile instance instead, and avoid wild
		// construction?
		return new UcmFile(this, object);
	}

	protected URI resolvePath(String path, boolean forced) throws UcmServiceException, UcmFileNotFoundException {
		path = UcmModel.sanitizePath(path);
		URI uri = this.uriByPaths.get(path);
		UcmTools data = null;
		// If we found no UcmGUID, or we already know it's a path not found,
		// but the user has requested we force a search regardless...
		if ((uri == null) || forced) {

			// First things first, clear out the old URI
			uri = null;

			// We use a lock to make sure we don't retrieve the same path "twice"...
			ReadWriteLock rwl = this.uriByPathsLocks.getLock(path);
			final Lock pathLock = rwl.writeLock();
			pathLock.lock();
			try {
				SessionWrapper<IdcSession> w = this.sessionFactory.acquireSession();
				IdcSession s = w.getWrapped();

				DataBinder binder = s.createBinder();
				binder.putLocal("IdcService", "FLD_INFO");
				binder.putLocal("path", path);
				ServiceResponse response = s.sendRequest(binder);
				DataBinder responseData = response.getResponseAsBinder();

				data = new UcmTools(responseData.getResultSet("FileInfo").getRows().get(0));
				String guid = data.getString(UcmAtt.dDocName);
				if (guid != null) {
					uri = new URI("file", guid, null);
				} else {
					// If we found no UcmGUID, it gets cached as a NULL guid
					guid = data.getString(UcmAtt.fFolderGUID);
					if (guid != null) {
						// If this was instead a folder, stash it as one...
						uri = new URI("folder", guid, null);
						this.uriByPaths.put(path, uri);
						// We reset it to null so the rest of the code handles it cleanly
						uri = null;
					}
				}
			} catch (final IdcClientException e) {
				// Is this a service exception from which we can identify that the item doesn't
				// exist?
				if (!ServiceException.class.isInstance(e)) {
					// No, this isn't an exception we can analyze...
					throw new UcmServiceException(String.format("Exception caught retrieving the file at [%s]", path),
						e);
				}

				// This may be an analyzable exception
				ServiceException se = ServiceException.class.cast(e);
				String mk = se.getBinder().getLocal("StatusMessageKey");
				if (!mk.startsWith("!csFldDoesNotExist,")) { throw new UcmServiceException(
					String.format("Exception caught retrieving the file at [%s]", path), e); }

			} catch (Exception e) {
				throw new UcmServiceException(String.format("Exception caught retrieving the file at [%s]", path), e);
			} finally {
				if (uri == null) {
					// Make sure we always have a URI value...
					uri = UcmModel.NULLURI;
				}
				this.uriByPaths.put(path, uri);
				pathLock.unlock();
			}
		}

		// TODO: We could arguably also stash away the DataObject, since FLD_INFO returns
		// a ton of extra info that we can leverage
		if (data != null) {
			// There was an object...should we stash it (perhaps on top of whatever's already
			// there?)
		}

		if (Tools.equals(UcmModel.NULLURI,
			uri)) { throw new UcmFileNotFoundException(String.format("The no object found at path [%s]", path)); }
		return uri;
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

	UcmFile getFile(UcmGUID guid) throws IdcClientException {
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

	UcmFolder getFolder(UcmGUID guid) throws IdcClientException {
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
}