package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

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

	// UcmGUID -> DataObject
	private CacheAccess<UcmGUID, DataObject> objectsByGUID = JCS.getInstance("ObjectsByGuid");
	private LockDispenser<UcmGUID, Lock> objectsByGUIDLocks = new LockDispenser<UcmGUID, Lock>() {
		@Override
		protected Lock newLock(UcmGUID key) {
			return new ReentrantLock();
		}
	};

	// Path mapping for file: path -> file://${dDocName}
	// Path mapping for folder: path -> folder://${fFolderGUID}
	// Null path mapping: path -> NULLID

	// path -> URI
	private CacheAccess<String, URI> uriByPaths = JCS.getInstance("GuidsByPath");
	private LockDispenser<String, Lock> uriByPathsLocks = new LockDispenser<String, Lock>() {
		@Override
		protected Lock newLock(String key) {
			return new ReentrantLock();
		}
	};

	// Child URI -> Parent URI
	private CacheAccess<URI, URI> parentsByURI = JCS.getInstance("ParentsByGuid");
	private LockDispenser<URI, Lock> parentsByURILocks = new LockDispenser<URI, Lock>() {
		@Override
		protected Lock newLock(URI key) {
			return new ReentrantLock();
		}
	};

	// URI -> List<UcmGUID>
	private CacheAccess<URI, List<UcmGUID>> historiesByContentID = JCS.getInstance("HistoryByContentId");
	private LockDispenser<URI, Lock> historiesByContentIDLocks = new LockDispenser<URI, Lock>() {
		@Override
		protected Lock newLock(URI key) {
			return new ReentrantLock();
		}
	};

	private final UcmSessionFactory sessionFactory;

	public UcmModel(UcmSessionFactory sessionFactory) {
		Objects.requireNonNull(sessionFactory, "Must provide a non-null session factory");
		this.sessionFactory = sessionFactory;
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
			Lock l = this.uriByPathsLocks.getLock(path);
			l.lock();
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
				l.unlock();
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