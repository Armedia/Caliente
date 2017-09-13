package com.armedia.caliente.engine.ucm.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;

public class FolderContentsIterator {

	private static final Pattern PATH_CHECKER = Pattern.compile("^(/|(/[^/]+)+/?)$");

	public static final FolderIteratorMode DEFAULT_MODE = FolderIteratorMode.COMBINED;

	public static final int MINIMUM_PAGE_SIZE = 1;
	public static final int DEFAULT_PAGE_SIZE = 1000;
	public static final int MAXIMUM_PAGE_SIZE = 100000;

	private final IdcSession session;
	private final String path;
	private final int pageSize;
	private final FolderIteratorMode folderIteratorMode;

	private int pageCount = -1;
	private int currentInPage = -1;

	private boolean firstRequestIssued = false;
	private UcmFolder folder = null;
	private DataObject localData = null;
	private Iterator<DataObject> folders = null;
	private Iterator<DataObject> files = null;

	private DataBinder requestBinder = null;
	private DataBinder responseBinder = null;

	private UcmFSObject current = null;
	private boolean completed = false;

	public FolderContentsIterator(IdcSession session, String path) {
		this(session, path, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(IdcSession session, String path, FolderIteratorMode folderIteratorMode) {
		this(session, path, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(IdcSession session, String path, int pageSize) {
		this(session, path, null, pageSize);
	}

	static String sanitizePath(String path) {
		Objects.requireNonNull(path, "Must provide a non-null path");
		if (!FolderContentsIterator.PATH_CHECKER.matcher(path).matches()) {
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

	public FolderContentsIterator(IdcSession session, String path, FolderIteratorMode folderIteratorMode,
		int pageSize) {
		Objects.requireNonNull(session, "Must provide a non-null session");
		this.path = FolderContentsIterator.sanitizePath(path);
		this.session = session;
		this.pageSize = Tools.ensureBetween(FolderContentsIterator.MINIMUM_PAGE_SIZE, pageSize,
			FolderContentsIterator.MAXIMUM_PAGE_SIZE);
		this.folderIteratorMode = Tools.coalesce(folderIteratorMode, FolderContentsIterator.DEFAULT_MODE);
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public IdcSession getSession() {
		return this.session;
	}

	public String getPath() {
		return this.path;
	}

	public int getPageCount() {
		return this.pageCount;
	}

	public int getCurrentInPage() {
		return this.currentInPage;
	}

	public int getCurrentPos() {
		return Math.max(-1, (this.pageCount * this.pageSize) + this.currentInPage);
	}

	private DataBinder getNextBatch() throws IdcClientException {
		if (this.requestBinder == null) {
			this.requestBinder = this.session.createBinder();
			this.requestBinder.putLocal("IdcService", "FLD_BROWSE");
			this.requestBinder.putLocal("path", this.path);
			this.folderIteratorMode.setParameters(this.requestBinder);
		}

		this.requestBinder.putLocal(this.folderIteratorMode.count, String.valueOf(this.pageSize));
		this.requestBinder.putLocal(this.folderIteratorMode.startRow, String.valueOf(this.pageSize * this.pageCount));

		return this.session.sendRequest(this.requestBinder).getResponseAsBinder();
	}

	public UcmFolder getFolder() throws IdcClientException {
		if (!this.firstRequestIssued) {
			hasNext();
		}
		return this.folder;
	}

	public DataObject getLocalData() throws IdcClientException {
		if (!this.firstRequestIssued) {
			hasNext();
		}
		return this.localData;
	}

	public boolean hasNext() throws IdcClientException {
		if (this.current != null) { return true; }
		if (this.completed) { return false; }

		// Do we need to page? If we do, retrieve the next page...
		if ((this.responseBinder == null) || ((this.currentInPage + 1) >= this.pageSize)) {
			// We need to page...
			this.pageCount++;
			this.responseBinder = getNextBatch();
			this.firstRequestIssued = true;

			if (this.localData == null) {
				this.localData = this.responseBinder.getLocalData();
			}

			if (this.folder == null) {
				DataResultSet rs = this.responseBinder.getResultSet("FolderInfo");
				if (rs != null) {
					List<DataObject> l = rs.getRows();
					if ((l != null) && !l.isEmpty()) {
						this.folder = new UcmFolder(l.get(0));
					}
				}
			}

			this.currentInPage = -1;
			DataResultSet rs = this.responseBinder.getResultSet("ChildFolders");
			if (rs != null) {
				this.folders = rs.getRows().iterator();
			} else {
				this.folders = Collections.emptyIterator();
			}

			rs = this.responseBinder.getResultSet("ChildFiles");
			if (rs != null) {
				this.files = rs.getRows().iterator();
			} else {
				this.files = Collections.emptyIterator();
			}
		}

		if (this.folders.hasNext()) {
			this.current = new UcmFolder(this.folder, this.folders.next());
			this.currentInPage++;
			return true;
		}

		if (this.files.hasNext()) {
			this.current = new UcmFile(this.folder, this.files.next());
			this.currentInPage++;
			return true;
		}

		// There are no more elements to be retrieved, so we're done...
		this.completed = true;
		this.responseBinder = null;
		this.requestBinder = null;
		this.current = null;
		this.folders = null;
		this.files = null;

		return false;
	}

	public UcmFSObject next() throws IdcClientException {
		if (!hasNext()) { throw new NoSuchElementException(); }
		UcmFSObject ret = this.current;
		this.current = null;
		return ret;
	}
}