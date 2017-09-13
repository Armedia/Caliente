package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
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

	private static final String NULLID = "idcnull";

	// GUID -> DataObject
	private static CacheAccess<String, DataObject> OBJECTS = JCS.getInstance("ObjectsByGuid");
	private static LockDispenser<String, Lock> OBJECTS_LOCKS = new LockDispenser<String, Lock>() {
		@Override
		protected Lock newLock(String key) {
			return new ReentrantLock();
		}
	};

	// PATH -> GUID
	private static CacheAccess<String, String> PATHS = JCS.getInstance("GuidsByPath");
	private static LockDispenser<String, Lock> PATHS_LOCKS = new LockDispenser<String, Lock>() {
		@Override
		protected Lock newLock(String key) {
			return new ReentrantLock();
		}
	};

	// Child GUID -> Parent GUID
	private static CacheAccess<String, String> PARENTS = JCS.getInstance("ParentsByGuid");
	private static LockDispenser<String, Lock> PARENTS_LOCKS = new LockDispenser<String, Lock>() {
		@Override
		protected Lock newLock(String key) {
			return new ReentrantLock();
		}
	};

	// dDocName -> List<GUID>
	private static CacheAccess<String, List<String>> HISTORY = JCS.getInstance("HistoryByContentId");
	private static LockDispenser<String, Lock> HISTORY_LOCKS = new LockDispenser<String, Lock>() {
		@Override
		protected Lock newLock(String key) {
			return new ReentrantLock();
		}
	};

	private final UcmSessionFactory sessionFactory;

	public UcmModel(UcmSessionFactory sessionFactory) {
		Objects.requireNonNull(sessionFactory, "Must provide a non-null session factory");
		this.sessionFactory = sessionFactory;
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
		path = UcmModel.sanitizePath(path);
		String guid = UcmModel.PATHS.get(path);
		if (guid == null) {
			// Ok...we haven't retrieved this path yet, so we first find the item...
			// We use a lock to make sure we don't retrieve the same path "twice"...
			Lock l = UcmModel.PATHS_LOCKS.getLock(guid);
			l.lock();
			try {
				SessionWrapper<IdcSession> w = this.sessionFactory.acquireSession();
				IdcSession s = w.getWrapped();

				DataBinder binder = s.createBinder();
				binder.putLocal("IdcService", "FLD_INFO");
				binder.putLocal("path", path);
				ServiceResponse response = s.sendRequest(binder);
				DataBinder responseData = response.getResponseAsBinder();

				// We found something...is it a file or a folder?

				return null;
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

				// Does not exist, so mark it as such...
				guid = UcmModel.NULLID;
			} catch (Exception e) {
				throw new UcmServiceException(String.format("Exception caught retrieving the file at [%s]", path), e);
			} finally {
				if (guid != null) {
					UcmModel.PATHS.put(guid, UcmModel.NULLID);
				}
				l.unlock();
			}
		}
		if (Tools.equals(UcmModel.NULLID,
			guid)) { throw new UcmFileNotFoundException(String.format("The file at path [%s] does not exist", path)); }

		// We have the GUID...go find
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

	UcmFolder getFolderByGUID(String guid) throws IdcClientException {
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