package com.armedia.caliente.engine.ucm.exporter;

import java.util.Collections;
import java.util.Iterator;
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

public class FolderContentsInterator {

	public static enum Mode {
		//
		COMBINED {
			@Override
			protected void setParameters(DataBinder requestBinder) {
				super.setParameters(requestBinder);
				requestBinder.putLocal("doCombinedBrowse", "1");
				requestBinder.putLocal("foldersFirst", "1");
			}
		}, //
		FILES, //
		FOLDERS, //
		//
		;

		private final String count;
		private final String startRow;

		private Mode() {
			String countLabel = name().toLowerCase();
			this.count = String.format("%sCount", countLabel);
			this.startRow = String.format("%sStartRow", countLabel);
		}

		protected void setParameters(DataBinder requestBinder) {
			requestBinder.putLocal("doRetrieveTargetInfo", "1");
		}
	}

	private static final Pattern PATH_CHECKER = Pattern.compile("^(/|(/[^/]+)+/?)$");

	public static final Mode DEFAULT_MODE = Mode.COMBINED;

	public static final int MINIMUM_PAGE_SIZE = 1;
	public static final int DEFAULT_PAGE_SIZE = 1000;
	public static final int MAXIMUM_PAGE_SIZE = 100000;

	private final IdcSession session;
	private final String path;
	private final int pageSize;
	private final Mode mode;

	private int pageCount = -1;
	private int currentInPage = -1;

	private Iterator<DataObject> folders = null;
	private Iterator<DataObject> files = null;

	private DataBinder requestBinder = null;
	private DataBinder responseBinder = null;

	private DataObject current = null;
	private boolean completed = false;

	public FolderContentsInterator(IdcSession session, String path) {
		this(session, path, null, FolderContentsInterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsInterator(IdcSession session, String path, Mode mode) {
		this(session, path, null, FolderContentsInterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsInterator(IdcSession session, String path, int pageSize) {
		this(session, path, null, pageSize);
	}

	private static String sanitizePath(String path) {
		Objects.requireNonNull(path, "Must provide a non-null path");
		if (!FolderContentsInterator.PATH_CHECKER.matcher(path).matches()) {
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

	public FolderContentsInterator(IdcSession session, String path, Mode mode, int pageSize) {
		Objects.requireNonNull(session, "Must provide a non-null session");
		this.path = FolderContentsInterator.sanitizePath(path);
		this.session = session;
		this.pageSize = Tools.ensureBetween(FolderContentsInterator.MINIMUM_PAGE_SIZE, pageSize,
			FolderContentsInterator.MAXIMUM_PAGE_SIZE);
		this.mode = Tools.coalesce(mode, FolderContentsInterator.DEFAULT_MODE);
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
			this.mode.setParameters(this.requestBinder);
		}

		this.requestBinder.putLocal(this.mode.count, String.valueOf(this.pageSize));
		this.requestBinder.putLocal(this.mode.startRow, String.valueOf(this.pageSize * this.pageCount));

		return this.session.sendRequest(this.requestBinder).getResponseAsBinder();
	}

	public boolean hasNext() throws IdcClientException {
		if (this.current != null) { return true; }
		if (this.completed) { return false; }

		// Do we need to page? If we do, retrieve the next page...
		if ((this.responseBinder == null) || ((this.currentInPage + 1) >= this.pageSize)) {
			// We need to page...
			this.pageCount++;
			this.responseBinder = getNextBatch();
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
			this.current = this.folders.next();
			this.currentInPage++;
			return true;
		}

		if (this.files.hasNext()) {
			this.current = this.files.next();
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

	public DataObject next() throws IdcClientException {
		if (!hasNext()) { throw new NoSuchElementException(); }
		DataObject ret = this.current;
		this.current = null;
		return ret;
	}
}