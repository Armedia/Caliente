package com.armedia.caliente.engine.local.exporter;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalFile;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.ArrayIterator;

public class LocalRecursiveIterator implements Iterator<ExportTarget> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private class RecursiveState {
		private final File base;
		private Iterator<File> childIterator = null;
		private LocalFile next = null;
		private int fileCount = 0;
		private int folderCount = 0;
		private boolean completed = false;

		private RecursiveState(File base) {
			this.base = base;
		}
	}

	private final boolean excludeEmptyFolders;
	private final LocalRoot root;

	private final Stack<RecursiveState> stateStack = new Stack<>();

	public LocalRecursiveIterator(LocalRoot root, boolean excludeEmptyFolders) throws IOException {
		this.root = root;
		this.stateStack.push(new RecursiveState(this.root.getFile()));
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
					state.childIterator = new ArrayIterator<>(children);
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
						this.log.trace("Found {} [{}]", f.isFile() ? "FILE" : "FOLDER", f.getAbsolutePath());
					}

					if (f.isDirectory()) {
						if (this.log.isTraceEnabled()) {
							this.log.trace("Recursing into [{}]", f.getAbsolutePath());
						}
						state.folderCount++;
						this.stateStack.push(new RecursiveState(f));
						continue recursion;
					}

					try {
						state.next = new LocalFile(this.root, f.getPath());
					} catch (IOException e) {
						throw new RuntimeException(
							String.format("Failed to relativize the path [%s] from [%s]", f, this.root), e);
					}
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
					try {
						state.next = new LocalFile(this.root, f.getPath());
					} catch (IOException e) {
						throw new RuntimeException(
							String.format("Failed to relativize the path [%s] from [%s]", f, this.root), e);
					}
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
		LocalFile ret = state.next;
		state.next = null;
		return new ExportTarget(ret.getAbsolute().isFile() ? CmfObject.Archetype.DOCUMENT : CmfObject.Archetype.FOLDER,
			ret.getId(), ret.getSafePath());
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}