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
package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.CloseableIteratorWrapper;
import com.armedia.commons.utilities.Tools;

public class LocalRecursiveIterator extends CloseableIterator<Path> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private class RecursiveState {
		private final Path base;
		private Iterator<Path> childIterator = null;
		private int fileCount = 0;
		private int folderCount = 0;
		private boolean completed = false;

		private RecursiveState(Path base) {
			this.base = base;
		}
	}

	private final boolean excludeEmptyFolders;
	private final LocalRoot root;

	private final Stack<RecursiveState> stateStack = new Stack<>();

	private final Predicate<Path> ignorePredicate;

	public LocalRecursiveIterator(LocalRoot root, boolean excludeEmptyFolders) {
		this(root, excludeEmptyFolders, null);
	}

	public LocalRecursiveIterator(LocalRoot root, boolean excludeEmptyFolders, Predicate<Path> ignorePredicate) {
		this.root = root;
		this.stateStack.push(new RecursiveState(this.root.getPath()));
		this.excludeEmptyFolders = excludeEmptyFolders;
		this.ignorePredicate = Tools.coalesce(ignorePredicate, Objects::isNull);
	}

	@Override
	protected Result findNext() throws Exception {
		recursion: while (!this.stateStack.isEmpty()) {
			RecursiveState state = this.stateStack.peek();
			// No next yet, go looking for it...
			final Path current = state.base;
			if (state.childIterator == null) {
				Stream<Path> s = Files.list(current).filter(this.ignorePredicate.negate());
				@SuppressWarnings("resource")
				Iterator<Path> it = new CloseableIteratorWrapper<>(s);
				if (it.hasNext()) {
					state.childIterator = it;
				} else {
					state.childIterator = null;
				}
			}

			if (state.childIterator != null) {
				while (state.childIterator.hasNext()) {
					final Path path = state.childIterator.next();
					if (this.log.isTraceEnabled()) {
						this.log.trace("Found {} [{}]", Files.isRegularFile(path) ? "FILE" : "FOLDER",
							path.toAbsolutePath());
					}

					if (Files.isDirectory(path)) {
						if (this.log.isTraceEnabled()) {
							this.log.trace("Recursing into [{}]", path.toAbsolutePath());
						}
						state.folderCount++;
						this.stateStack.push(new RecursiveState(path));
						continue recursion;
					}

					state.fileCount++;
					return found(path);
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
					return found(state.base);
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