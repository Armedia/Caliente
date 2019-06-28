/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.cmis;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmisRecursiveIterator implements Iterator<CmisObject> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private class RecursiveState {
		private final Folder base;
		private Iterator<CmisObject> childIterator = null;
		private CmisObject next = null;
		private int fileCount = 0;
		private int folderCount = 0;
		private boolean completed = false;

		private RecursiveState(Folder base) {
			this.base = base;
		}
	}

	private final boolean excludeEmptyFolders;
	private final OperationContext ctx;

	private final Stack<RecursiveState> stateStack = new Stack<>();

	public CmisRecursiveIterator(Session session, Folder root, boolean excludeEmptyFolders) {
		this(session, root, excludeEmptyFolders, null);
	}

	public CmisRecursiveIterator(Session session, Folder root, boolean excludeEmptyFolders, OperationContext ctx) {
		this.stateStack.push(new RecursiveState(root));
		this.excludeEmptyFolders = excludeEmptyFolders;
		this.ctx = ctx;

	}

	private boolean ignoreObject(CmisObject object) {
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
			if (state.childIterator == null) {
				ItemIterable<CmisObject> children = (this.ctx != null ? current.getChildren(this.ctx)
					: current.getChildren());
				state.childIterator = new CmisPagingIterator<>(children);
			}
			while (state.childIterator.hasNext()) {
				CmisObject f = state.childIterator.next();
				if (ignoreObject(f)) {
					continue;
				}

				final String fullPath;
				if (f instanceof FileableCmisObject) {
					FileableCmisObject F = FileableCmisObject.class.cast(f);
					List<String> paths = F.getPaths();
					if (!paths.isEmpty()) {
						fullPath = paths.get(0);
					} else {
						fullPath = String.format("${unfiled}:%s", f.getName());
					}
				} else {
					fullPath = String.format("${unfiled}:%s", f.getName());
				}
				if (this.log.isTraceEnabled()) {
					this.log.trace("Found {} [{}]", f.getType().getId(), fullPath);
				}

				if (f instanceof Folder) {
					if (this.log.isTraceEnabled()) {
						this.log.trace("Recursing into {} [{}]", f.getType().getId(), fullPath);
					}
					state.folderCount++;
					this.stateStack.push(new RecursiveState(Folder.class.cast(f)));
					continue recursion;
				}

				state.next = f;
				state.fileCount++;
				return true;
			}

			// If we're not excluding empty folders, and this is an empty folder, then we queue it
			// up for retrieval...but we only do it once
			if (!state.completed) {
				state.completed = true;
				if (!this.excludeEmptyFolders && ((state.fileCount | state.folderCount) == 0)) {
					CmisObject f = state.base;
					state.next = f;
					return true;
				}
			}
			this.stateStack.pop();
		}
		return false;
	}

	@Override
	public CmisObject next() {
		if (!hasNext()) { throw new NoSuchElementException(); }
		RecursiveState state = this.stateStack.peek();
		CmisObject ret = state.next;
		state.next = null;
		return ret;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}