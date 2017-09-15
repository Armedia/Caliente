package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;

public class FolderContentsIterator {

	public static final FolderIteratorMode DEFAULT_MODE = FolderIteratorMode.COMBINED;

	public static final int MINIMUM_PAGE_SIZE = 1;
	public static final int DEFAULT_PAGE_SIZE = 1000;
	public static final int MAXIMUM_PAGE_SIZE = 100000;

	private final UcmSession session;
	private final FolderLocatorMode folderLocatorMode;
	private final Object searchKey;
	private final int pageSize;
	private final FolderIteratorMode folderIteratorMode;

	private int pageCount = -1;
	private int currentInPage = -1;

	private boolean firstRequestIssued = false;
	private UcmAttributes folder = null;
	private UcmAttributes localData = null;
	private String parentPath = null;
	private Iterator<DataObject> folders = null;
	private Iterator<DataObject> files = null;

	private DataBinder requestBinder = null;
	private DataBinder responseBinder = null;

	private UcmAttributes current = null;
	private boolean completed = false;

	public FolderContentsIterator(UcmSession session, String path) {
		this(session, FolderLocatorMode.BY_PATH, path, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, String path, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_PATH, path, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, String path, int pageSize) {
		this(session, FolderLocatorMode.BY_PATH, path, null, pageSize);
	}

	public FolderContentsIterator(UcmSession session, String path, FolderIteratorMode folderIteratorMode,
		int pageSize) {
		this(session, FolderLocatorMode.BY_PATH, path, folderIteratorMode, pageSize);
	}

	public FolderContentsIterator(UcmSession session, UcmGUID guid) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, UcmGUID guid, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, UcmGUID guid, int pageSize) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, pageSize);
	}

	public FolderContentsIterator(UcmSession session, UcmGUID guid, FolderIteratorMode folderIteratorMode,
		int pageSize) {
		this(session, FolderLocatorMode.BY_GUID, guid, folderIteratorMode, pageSize);
	}

	public FolderContentsIterator(UcmSession session, URI uri) {
		this(session, FolderLocatorMode.BY_URI, uri, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, URI uri, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_URI, uri, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, URI uri, int pageSize) {
		this(session, FolderLocatorMode.BY_URI, uri, null, pageSize);
	}

	public FolderContentsIterator(UcmSession session, URI uri, FolderIteratorMode folderIteratorMode, int pageSize) {
		this(session, FolderLocatorMode.BY_URI, uri, folderIteratorMode, pageSize);
	}

	FolderContentsIterator(UcmSession session, FolderLocatorMode folderLocatorMode, Object searchKey,
		FolderIteratorMode folderIteratorMode, int pageSize) {
		Objects.requireNonNull(session, "Must provide a non-null session");
		Objects.requireNonNull(searchKey, "Must provide a non-null search criterion");
		this.folderLocatorMode = folderLocatorMode;
		this.searchKey = folderLocatorMode.sanitizeKey(searchKey);
		this.session = session;
		this.pageSize = Tools.ensureBetween(FolderContentsIterator.MINIMUM_PAGE_SIZE, pageSize,
			FolderContentsIterator.MAXIMUM_PAGE_SIZE);
		this.folderIteratorMode = Tools.coalesce(folderIteratorMode, FolderContentsIterator.DEFAULT_MODE);
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public UcmSession getSession() {
		return this.session;
	}

	public FolderLocatorMode getSearchMode() {
		return this.folderLocatorMode;
	}

	public Object getSearchKey() {
		return this.searchKey;
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
			this.folderLocatorMode.applySearchParameters(this.requestBinder, this.searchKey);
			this.folderIteratorMode.setParameters(this.requestBinder);
			this.requestBinder.putLocal("IdcService", "FLD_BROWSE");
		}

		this.requestBinder.putLocal(this.folderIteratorMode.count, String.valueOf(this.pageSize));
		this.requestBinder.putLocal(this.folderIteratorMode.startRow, String.valueOf(this.pageSize * this.pageCount));

		return this.session.sendRequest(this.requestBinder).getResponseAsBinder();
	}

	public UcmAttributes getFolder() throws IdcClientException {
		if (!this.firstRequestIssued) {
			hasNext();
		}
		return this.folder;
	}

	public UcmAttributes getLocalData() throws IdcClientException {
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
				DataObject localData = this.responseBinder.getLocalData();
				this.localData = new UcmAttributes(localData);
				this.parentPath = localData.get("folderPath");
			}

			if (this.folder == null) {
				DataResultSet rs = this.responseBinder.getResultSet("FolderInfo");
				if (rs != null) {
					List<DataObject> l = rs.getRows();
					if ((l != null) && !l.isEmpty()) {
						this.folder = new UcmAttributes(l.get(0));
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

		if (this.folders.hasNext() || this.files.hasNext()) {
			DataObject o = (this.folders.hasNext() ? this.folders.next() : this.files.next());
			Map<String, String> m = new HashMap<>(o);
			m.put(UcmAtt.$ucmParentPath.name(), this.parentPath);
			this.current = new UcmAttributes(m);
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

	public UcmAttributes next() throws IdcClientException {
		if (!hasNext()) { throw new NoSuchElementException(); }
		UcmAttributes ret = this.current;
		this.current = null;
		return ret;
	}
}