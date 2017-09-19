package com.armedia.caliente.engine.ucm.model;

import java.io.Serializable;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.commons.utilities.Tools;

public class FolderTreeIterator {

	public static final class Config implements Serializable {
		private static final long serialVersionUID = 1L;

		private int pageSize = FolderTreeIterator.DEFAULT_PAGE_SIZE;
		private FolderIteratorMode mode = FolderTreeIterator.DEFAULT_MODE;
		private boolean recurseShortcuts = FolderTreeIterator.DEFAULT_RECURSE_SHORTCUTS;

		public int getPageSize() {
			return this.pageSize;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public FolderIteratorMode getMode() {
			return this.mode;
		}

		public void setMode(FolderIteratorMode mode) {
			this.mode = mode;
		}

		public boolean isRecurseShortcuts() {
			return this.recurseShortcuts;
		}

		public void setRecurseShortcuts(boolean recurseShortcuts) {
			this.recurseShortcuts = recurseShortcuts;
		}
	}

	private static final Config DEFAULT_CONFIG = new Config();

	public static final FolderIteratorMode DEFAULT_MODE = FolderIteratorMode.COMBINED;

	public static final boolean DEFAULT_RECURSE_SHORTCUTS = true;

	public static final int MINIMUM_PAGE_SIZE = 1;
	public static final int DEFAULT_PAGE_SIZE = 1000;
	public static final int MAXIMUM_PAGE_SIZE = 100000;

	private final UcmSession session;
	private final FolderLocatorMode searchMode;
	private final Object searchKey;
	private final int pageSize;
	private final FolderIteratorMode mode;
	private final boolean recurseShortcuts;

	private boolean rootExamined = false;
	private UcmAttributes current = null;

	private Stack<FolderContentsIterator> recursion = new Stack<>();

	public FolderTreeIterator(UcmSession session, String path) {
		this(session, FolderLocatorMode.BY_PATH, path, null);
	}

	public FolderTreeIterator(UcmSession session, URI uri) {
		this(session, FolderLocatorMode.BY_URI, uri, null);
	}

	public FolderTreeIterator(UcmSession session, UcmUniqueURI guid) {
		this(session, FolderLocatorMode.BY_GUID, guid, null);
	}

	public FolderTreeIterator(UcmSession session, String path, Config config) {
		this(session, FolderLocatorMode.BY_PATH, path, config);
	}

	public FolderTreeIterator(UcmSession session, URI uri, Config config) {
		this(session, FolderLocatorMode.BY_URI, uri, config);
	}

	public FolderTreeIterator(UcmSession session, UcmUniqueURI guid, Config config) {
		this(session, FolderLocatorMode.BY_GUID, guid, config);
	}

	private FolderTreeIterator(UcmSession session, FolderLocatorMode searchMode, Object key, Config config) {
		Objects.requireNonNull(session, "Must provide a non-null session");
		this.searchKey = searchMode.sanitizeKey(key);
		this.searchMode = searchMode;
		this.session = session;
		if (config == null) {
			config = FolderTreeIterator.DEFAULT_CONFIG;
		}
		this.pageSize = Tools.ensureBetween(FolderContentsIterator.MINIMUM_PAGE_SIZE, config.pageSize,
			FolderContentsIterator.MAXIMUM_PAGE_SIZE);
		this.mode = Tools.coalesce(config.mode, FolderContentsIterator.DEFAULT_MODE);
		this.recursion.push(new FolderContentsIterator(session, searchMode, this.searchKey, FolderIteratorMode.COMBINED,
			this.pageSize));
		this.recurseShortcuts = config.recurseShortcuts;
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

			// Special edge case - if we're in the first recursion and we're not recursing
			// shortcuts, and the first recursion is a shortcut, we quite simply don't move past
			if ((this.recursion.size() == 1) && UcmModel.isShortcut(currentRecursion.getFolder())
				&& !this.recurseShortcuts) { return false; }

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
				if (!UcmModel.isShortcut(att) || this.recurseShortcuts) {
					// We only recurse if it's not a shortcut
					this.recursion.push(new FolderContentsIterator(this.session, FolderLocatorMode.BY_URI, uri,
						FolderIteratorMode.COMBINED, this.pageSize));
				}
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