package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionException;
import com.armedia.cmf.engine.sharepoint.types.ShptFile;
import com.armedia.cmf.engine.sharepoint.types.ShptFolder;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.File;
import com.independentsoft.share.Folder;

public class ShptRecursiveIterator implements Iterator<ExportTarget> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static class EmptyIterator<T> implements Iterator<T> {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public T next() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private class RecursiveState {
		private final Folder base;
		private Iterator<File> fileIterator = null;
		private Iterator<Folder> folderIterator = null;
		private ExportTarget next = null;
		private int fileCount = 0;
		private int folderCount = 0;
		private boolean completed = false;

		private RecursiveState(Folder base) {
			this.base = base;
		}
	}

	private static final Iterator<File> EMPTY_FILE_ITERATOR = new EmptyIterator<File>();
	private static final Iterator<Folder> EMPTY_FOLDER_ITERATOR = new EmptyIterator<Folder>();

	private final ShptExportDelegateFactory factory;
	private final ShptSession service;
	private final boolean excludeEmptyFolders;

	private final Stack<RecursiveState> stateStack = new Stack<RecursiveState>();

	public ShptRecursiveIterator(ShptExportDelegateFactory factory, ShptSession service, Folder root,
		CfgTools configuration, boolean excludeEmptyFolders) {
		this.factory = factory;
		this.service = service;
		this.stateStack.push(new RecursiveState(root));
		this.excludeEmptyFolders = excludeEmptyFolders;
		this.log.debug("Starting recursive search of [{}]...", root.getServerRelativeUrl());
	}

	private boolean ignorePath(String path) {
		// TODO: A configuration setting "somewhere" should control which paths we ignore
		return false;
	}

	@Override
	public boolean hasNext() {
		recursion: while (!this.stateStack.isEmpty()) {
			RecursiveState state = this.stateStack.peek();
			if (state.next != null) { return true; }

			// No next yet, go looking for it...
			final Folder current = state.base;
			if (state.fileIterator == null) {
				Collection<File> c;
				try {
					c = this.service.getFiles(current.getServerRelativeUrl());
				} catch (ShptSessionException e) {
					throw new RuntimeException(String.format("Exception caught getting the file list for [%s]",
						current.getServerRelativeUrl()), e);
				}

				if ((c != null) && !c.isEmpty()) {
					state.fileIterator = c.iterator();
				} else {
					state.fileIterator = ShptRecursiveIterator.EMPTY_FILE_ITERATOR;
				}
			}
			if (state.fileIterator.hasNext()) {
				File f = state.fileIterator.next();
				ShptFile F;
				try {
					F = new ShptFile(this.factory, f);
				} catch (Exception e) {
					throw new RuntimeException(String.format("Failed to create a new ShptFile instance for file [%s]",
						f.getServerRelativeUrl()));
				}
				this.log.debug("\tFound file: [{}]", f.getServerRelativeUrl());
				state.next = new ExportTarget(StoredObjectType.DOCUMENT, F.getObjectId(), F.getSearchKey());
				state.fileCount++;
				return true;
			}

			if (state.folderIterator == null) {
				Collection<Folder> c;
				try {
					c = this.service.getFolders(current.getServerRelativeUrl());
				} catch (ShptSessionException e) {
					throw new RuntimeException(String.format("Exception caught getting the folder list for [%s]",
						current.getServerRelativeUrl()), e);
				}
				if ((c != null) && !c.isEmpty()) {
					state.folderIterator = c.iterator();
				} else {
					state.folderIterator = ShptRecursiveIterator.EMPTY_FOLDER_ITERATOR;
				}
			}

			inner: while (state.folderIterator.hasNext()) {
				Folder f = state.folderIterator.next();
				if (ignorePath(f.getServerRelativeUrl())) {
					continue inner;
				}
				state.folderCount++;
				this.log.debug("\tExporting the contents of folder: [{}]", f.getServerRelativeUrl());
				this.stateStack.push(new RecursiveState(f));
				continue recursion;
			}

			// If we're not excluding empty folders, and this is an empty folder, then we queue it
			// up for retrieval...but we only do it once
			if (!state.completed) {
				state.completed = true;
				if (!this.excludeEmptyFolders && ((state.fileCount | state.folderCount) == 0)) {
					Folder f = state.base;
					ShptFolder F;
					try {
						F = new ShptFolder(this.factory, f);
					} catch (Exception e) {
						throw new RuntimeException(String.format(
							"Failed to create a new ShptFolder instance for folder [%s]", f.getServerRelativeUrl()));
					}
					this.log.debug("\tExporting the contents of folder: [{}]", f.getServerRelativeUrl());
					state.next = new ExportTarget(StoredObjectType.FOLDER, F.getObjectId(), F.getSearchKey());
					return true;
				}
			}
			this.stateStack.pop();
		}
		return false;
	}

	@Override
	public ExportTarget next() {
		if (!hasNext()) { throw new NoSuchElementException(); }
		RecursiveState state = this.stateStack.peek();
		ExportTarget ret = state.next;
		state.next = null;
		return ret;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}