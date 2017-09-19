package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.commons.utilities.Tools;

public class FolderTreeIterator {

	public static final FolderIteratorMode DEFAULT_MODE = FolderIteratorMode.COMBINED;

	public static final int MINIMUM_PAGE_SIZE = 1;
	public static final int DEFAULT_PAGE_SIZE = 1000;
	public static final int MAXIMUM_PAGE_SIZE = 100000;

	private final UcmSession session;
	private final FolderLocatorMode searchMode;
	private final Object searchKey;
	private final int pageSize;
	private final FolderIteratorMode mode;

	private boolean rootExamined = false;
	private UcmAttributes current = null;

	private Stack<FolderContentsIterator> recursion = new Stack<>();

	public FolderTreeIterator(UcmSession session, String path) {
		this(session, FolderLocatorMode.BY_PATH, path, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(UcmSession session, String path, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_PATH, path, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(UcmSession session, String path, int pageSize) {
		this(session, FolderLocatorMode.BY_PATH, path, null, pageSize);
	}

	public FolderTreeIterator(UcmSession session, String path, FolderIteratorMode mode, int pageSize) {
		this(session, FolderLocatorMode.BY_PATH, path, mode, pageSize);
	}

	public FolderTreeIterator(UcmSession session, URI uri) {
		this(session, FolderLocatorMode.BY_URI, uri, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(UcmSession session, URI uri, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_URI, uri, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(UcmSession session, URI uri, int pageSize) {
		this(session, FolderLocatorMode.BY_URI, uri, null, pageSize);
	}

	public FolderTreeIterator(UcmSession session, URI uri, FolderIteratorMode mode, int pageSize) {
		this(session, FolderLocatorMode.BY_URI, uri, mode, pageSize);
	}

	public FolderTreeIterator(UcmSession session, UcmUniqueURI guid) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(UcmSession session, UcmUniqueURI guid, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, FolderContentsIterator.DEFAULT_PAGE_SIZE);
	}

	public FolderTreeIterator(UcmSession session, UcmUniqueURI guid, int pageSize) {
		this(session, FolderLocatorMode.BY_GUID, guid, null, pageSize);
	}

	public FolderTreeIterator(UcmSession session, UcmUniqueURI guid, FolderIteratorMode mode, int pageSize) {
		this(session, FolderLocatorMode.BY_GUID, guid, mode, pageSize);
	}

	private FolderTreeIterator(UcmSession session, FolderLocatorMode searchMode, Object key, FolderIteratorMode mode,
		int pageSize) {
		Objects.requireNonNull(session, "Must provide a non-null session");
		this.searchKey = searchMode.sanitizeKey(key);
		this.searchMode = searchMode;
		this.session = session;
		this.pageSize = Tools.ensureBetween(FolderContentsIterator.MINIMUM_PAGE_SIZE, pageSize,
			FolderContentsIterator.MAXIMUM_PAGE_SIZE);
		this.mode = Tools.coalesce(mode, FolderContentsIterator.DEFAULT_MODE);
		this.recursion.push(
			new FolderContentsIterator(session, searchMode, this.searchKey, FolderIteratorMode.COMBINED, pageSize));
	}

	public UcmSession getSession() {
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

	public boolean hasNext() throws UcmServiceException {
		if (this.current != null) { return true; }

		nextLevel: while (!this.recursion.isEmpty()) {
			final FolderContentsIterator currentRecursion = this.recursion.peek();

			if (!this.rootExamined) {
				// This only needs to be done for the root folder of the entire
				// tree, since all child folders will be iterated over as a matter
				// of course during the normal algorithm.
				if (this.mode != FolderIteratorMode.FILES) {
					this.current = currentRecursion.getFolder();
				}
				this.rootExamined = true;
				if (this.current != null) { return true; }
			}

			if (!currentRecursion.hasNext()) {
				// If this level is exhausted, we move to the next level...
				this.recursion.pop();
				continue;
			}

			UcmAttributes att = currentRecursion.next();

			URI uri = UcmModel.getURI(att);
			if (UcmModel.isFolderURI(uri)) {
				// If this is a folder, we return it, but we store the recursion for the next
				// call to hasNext()
				this.recursion.push(new FolderContentsIterator(this.session, FolderLocatorMode.BY_URI, uri,
					FolderIteratorMode.COMBINED, this.pageSize));
				if (this.mode == FolderIteratorMode.FILES) {
					// We're not supposed to iterate over folders, so we simply loop back up
					continue nextLevel;
				}
			} else {
				if (this.mode == FolderIteratorMode.FOLDERS) {
					// We're not supposed to iterate over files, so we simply loop back up
					continue nextLevel;
				}
			}
			this.current = att;
			return true;
		}
		return false;
	}

	public UcmAttributes next() throws UcmServiceException {
		if (!hasNext()) { throw new NoSuchElementException(); }
		UcmAttributes ret = this.current;
		this.current = null;
		return ret;
	}
}