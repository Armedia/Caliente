package com.armedia.cmf.engine.local.exporter;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.ArrayIterator;

public class LocalRecursiveIterator implements Iterator<ExportTarget> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private class RecursiveState {
		private final File base;
		private Iterator<File> childIterator = null;
		private File next = null;
		private int fileCount = 0;
		private int folderCount = 0;
		private boolean completed = false;

		private RecursiveState(File base) {
			this.base = base;
		}
	}

	private final boolean excludeEmptyFolders;
	private final File root;

	private final Stack<RecursiveState> stateStack = new Stack<RecursiveState>();

	public LocalRecursiveIterator(File root, boolean excludeEmptyFolders) throws IOException {
		this.root = root.getCanonicalFile();
		this.stateStack.push(new RecursiveState(this.root));
		this.excludeEmptyFolders = excludeEmptyFolders;
	}

	private boolean ignoreObject(File object) {
		// TODO: A configuration setting "somewhere" should control which paths we ignore
		return false;
	}

	@Override
	public boolean hasNext() {
		recursion: while (!this.stateStack.isEmpty()) {
			RecursiveState state = this.stateStack.peek();
			if (state.next != null) { return true; }

			// No next yet, go looking for it...
			final File current = state.base;
			if (state.childIterator == null) {
				File[] children = current.listFiles();
				if ((children != null) && (children.length > 0)) {
					state.childIterator = new ArrayIterator<File>(children);
				} else {
					state.childIterator = null;
				}
			}

			if (state.childIterator != null) {
				while (state.childIterator.hasNext()) {
					File f = state.childIterator.next();
					if (ignoreObject(f)) {
						continue;
					}

					if (this.log.isTraceEnabled()) {
						this.log.trace(String.format("Found %s [%s]", f.isFile() ? "FILE" : "FOLDER",
							f.getAbsolutePath()));
					}

					if (f.isDirectory()) {
						if (this.log.isTraceEnabled()) {
							this.log.trace(String.format("Recursing into [%s]", f.getAbsolutePath()));
						}
						state.folderCount++;
						this.stateStack.push(new RecursiveState(f));
						continue recursion;
					}

					state.next = f;
					state.fileCount++;
					return true;
				}
			}

			// If we're not excluding empty folders, and this is an empty folder, then we queue it
			// up for retrieval...but we only do it once
			if (!state.completed) {
				state.completed = true;
				if (!this.excludeEmptyFolders && ((state.fileCount | state.folderCount) == 0)) {
					File f = state.base;
					state.next = f;
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
		File ret = state.next;
		state.next = null;
		String path;
		String id;
		try {
			path = LocalExportDelegate.calculateRelativePath(this.root, ret);
			id = LocalExportDelegate.calculateCanonicalPathHash(this.root, ret);
		} catch (IOException e) {
			throw new RuntimeException(String.format(
				"Failed to calculate the path ID or relative path for [%s] from [%s]", ret, this.root), e);
		}
		return new ExportTarget(ret.isFile() ? StoredObjectType.DOCUMENT : StoredObjectType.FOLDER, id, path);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}