package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Objects;
import java.util.Stack;

import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataObject;

public class FolderTreeIterator {

	public static final FolderIteratorMode DEFAULT_MODE = FolderIteratorMode.COMBINED;

	public static final int MINIMUM_PAGE_SIZE = 1;
	public static final int DEFAULT_PAGE_SIZE = 1000;
	public static final int MAXIMUM_PAGE_SIZE = 100000;

	private final IdcSession session;
	private final FolderLocatorMode searchMode;
	private final Object searchKey;
	private final int pageSize;
	private final FolderIteratorMode mode;

	private Stack<FolderContentsIterator> recursion = new Stack<>();

	public FolderTreeIterator(IdcSession session, String path) {
		this(session, FolderLocatorMode.BY_PATH, path, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(IdcSession session, String path, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_PATH, path, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(IdcSession session, String path, int pageSize) {
		this(session, FolderLocatorMode.BY_PATH, path, null, pageSize);
	}

	public FolderTreeIterator(IdcSession session, String path, FolderIteratorMode mode, int pageSize) {
		this(session, FolderLocatorMode.BY_PATH, path, mode, pageSize);
	}

	public FolderTreeIterator(IdcSession session, URI uri) {
		this(session, FolderLocatorMode.BY_URI, uri, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(IdcSession session, URI uri, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_URI, uri, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(IdcSession session, URI uri, int pageSize) {
		this(session, FolderLocatorMode.BY_URI, uri, null, pageSize);
	}

	public FolderTreeIterator(IdcSession session, URI uri, FolderIteratorMode mode, int pageSize) {
		this(session, FolderLocatorMode.BY_URI, uri, mode, pageSize);
	}

	public FolderTreeIterator(IdcSession session, UcmGUID guid) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(IdcSession session, UcmGUID guid, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(IdcSession session, UcmGUID guid, int pageSize) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, pageSize);
	}

	public FolderTreeIterator(IdcSession session, UcmGUID guid, FolderIteratorMode mode, int pageSize) {
		this(session, FolderLocatorMode.BY_GUID, guid, mode, pageSize);
	}

	private FolderTreeIterator(IdcSession session, FolderLocatorMode searchMode, Object key,
		FolderIteratorMode mode, int pageSize) {
		Objects.requireNonNull(session, "Must provide a non-null session");
		this.searchKey = searchMode.sanitizeKey(key);
		this.searchMode = searchMode;
		this.session = session;
		this.pageSize = Tools.ensureBetween(FolderContentsIterator.MINIMUM_PAGE_SIZE, pageSize,
			FolderContentsIterator.MAXIMUM_PAGE_SIZE);
		this.mode = Tools.coalesce(mode, FolderContentsIterator.DEFAULT_MODE);
		this.recursion.push(new FolderContentsIterator(session, searchMode, this.searchKey, mode, pageSize));
	}

	public IdcSession getSession() {
		return this.session;
	}

	public FolderLocatorMode getSearchMode() {
		return this.searchMode;
	}

	public Object getSearchKey() {
		return this.searchKey;
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public FolderIteratorMode getMode() {
		return this.mode;
	}

	public boolean hasNext() throws IdcClientException {
		while (!this.recursion.isEmpty()) {
			final FolderContentsIterator current = this.recursion.peek();
			if (!current.hasNext()) {
				this.recursion.pop();
				continue;
			}
			return true;
		}
		return false;
	}

	public DataObject next() throws IdcClientException {
		return null;
	}
}