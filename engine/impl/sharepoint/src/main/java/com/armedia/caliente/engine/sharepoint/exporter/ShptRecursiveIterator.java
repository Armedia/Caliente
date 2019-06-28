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
package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionException;
import com.armedia.caliente.store.CmfObject;
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

	private static final Iterator<File> EMPTY_FILE_ITERATOR = new EmptyIterator<>();
	private static final Iterator<Folder> EMPTY_FOLDER_ITERATOR = new EmptyIterator<>();

	private final ShptSession service;
	private final boolean excludeEmptyFolders;

	private final Stack<RecursiveState> stateStack = new Stack<>();

	public ShptRecursiveIterator(ShptSession service, Folder root, CfgTools configuration,
		boolean excludeEmptyFolders) {
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
				final String objectId = ShptFile.doCalculateObjectId(f);
				final String searchKey = ShptFile.doCalculateSearchKey(f);
				this.log.debug("\tFound file: [{}]", f.getServerRelativeUrl());
				state.next = new ExportTarget(CmfObject.Archetype.DOCUMENT, objectId, searchKey);
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
					final String objectId = ShptFolder.doCalculateObjectId(f);
					final String searchKey = ShptFolder.doCalculateSearchKey(f);
					this.log.debug("\tExporting the contents of folder: [{}]", f.getServerRelativeUrl());
					state.next = new ExportTarget(CmfObject.Archetype.FOLDER, objectId, searchKey);
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