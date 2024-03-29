/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.sql.exporter;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.sql.common.SqlFile;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.commons.utilities.ArrayIterator;
import com.armedia.commons.utilities.CloseableIterator;

public class SqlRecursiveIterator extends CloseableIterator<SqlFile> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private class RecursiveState {
		private final File base;
		private Iterator<File> childIterator = null;
		private int fileCount = 0;
		private int folderCount = 0;
		private boolean completed = false;

		private RecursiveState(File base) {
			this.base = base;
		}
	}

	private final boolean excludeEmptyFolders;
	private final SqlRoot root;

	private final Stack<RecursiveState> stateStack = new Stack<>();

	public SqlRecursiveIterator(SqlRoot root, boolean excludeEmptyFolders) throws IOException {
		this.root = root;
		this.stateStack.push(new RecursiveState(this.root.getFile()));
		this.excludeEmptyFolders = excludeEmptyFolders;
	}

	private boolean ignoreObject(File object) {
		// TODO: A configuration setting "somewhere" should control which paths we ignore
		return false;
	}

	@Override
	protected Result findNext() throws Exception {
		recursion: while (!this.stateStack.isEmpty()) {
			RecursiveState state = this.stateStack.peek();
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
						SqlFile next = new SqlFile(this.root, f.getPath());
						state.fileCount++;
						return found(next);
					} catch (IOException e) {
						throw new RuntimeException(
							String.format("Failed to relativize the path [%s] from [%s]", f, this.root), e);
					}
				}
			}

			// We're done with this folder, so whatever happens we're removing it from the stack
			this.stateStack.pop();

			// If this was the root search element, we return nothing b/c we're done here...
			if (this.stateStack.isEmpty()) { return null; }

			// If we're not excluding empty folders, and this is an empty folder, then we queue it
			// up for retrieval...but we only do it once
			if (!state.completed) {
				state.completed = true;
				if (!this.excludeEmptyFolders || ((state.fileCount | state.folderCount) != 0)) {
					File f = state.base;
					try {
						return found(new SqlFile(this.root, f.getPath()));
					} catch (IOException e) {
						throw new RuntimeException(
							String.format("Failed to relativize the path [%s] from [%s]", f, this.root), e);
					}
				}
			}
		}
		// No more recursions... we're done!
		return null;
	}

	@Override
	protected void doClose() throws Exception {
	}
}