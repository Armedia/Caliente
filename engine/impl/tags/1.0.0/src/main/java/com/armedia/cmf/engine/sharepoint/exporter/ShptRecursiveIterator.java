package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.types.ShptFile;
import com.armedia.cmf.engine.sharepoint.types.ShptFolder;
import com.armedia.cmf.storage.StoredObjectType;
import com.independentsoft.share.File;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;

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

	private final Service service;
	private final boolean excludeEmptyFolders;
	private final String ctsPath;

	private final Stack<RecursiveState> stateStack = new Stack<RecursiveState>();

	public ShptRecursiveIterator(Service service, Folder root, boolean excludeEmptyFolders) {
		this.service = service;
		this.stateStack.push(new RecursiveState(root));
		this.excludeEmptyFolders = excludeEmptyFolders;
		this.ctsPath = String.format("%s_cts", root.getServerRelativeUrl());
		this.log.trace("Starting recursive search of [{}]...", root.getServerRelativeUrl());
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
				} catch (ServiceException e) {
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
				ShptFile F = new ShptFile(this.service, f);
				this.log.trace("\tFound file: [{}]", f.getServerRelativeUrl());
				state.next = new ExportTarget(StoredObjectType.DOCUMENT, F.getId(), F.getSearchKey());
				state.fileCount++;
				return true;
			}

			if (state.folderIterator == null) {
				Collection<Folder> c;
				try {
					c = this.service.getFolders(current.getServerRelativeUrl());
				} catch (ServiceException e) {
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
				// TODO: Perhaps generalize this to have a list of paths we're not interested in
				if (f.getServerRelativeUrl().startsWith(this.ctsPath)) {
					continue inner;
				}
				state.folderCount++;
				this.stateStack.push(new RecursiveState(f));
				continue recursion;
			}

			// If we're not excluding empty folders, and this is an empty folder, then we queue it
			// up for retrieval...but we only do it once
			if (!state.completed) {
				state.completed = true;
				if (!this.excludeEmptyFolders && ((state.fileCount | state.folderCount) == 0)) {
					Folder f = state.base;
					ShptFolder F = new ShptFolder(this.service, f);
					this.log.trace("\tExporting the contents of folder: [{}]", f.getServerRelativeUrl());
					state.next = new ExportTarget(StoredObjectType.FOLDER, F.getId(), F.getSearchKey());
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