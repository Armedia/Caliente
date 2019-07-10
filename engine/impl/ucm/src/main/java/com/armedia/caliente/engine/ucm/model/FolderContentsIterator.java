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
package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.armedia.caliente.engine.ucm.UcmConstants;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.model.DataResultSet.Field;

public class FolderContentsIterator {

	public static final FolderIteratorMode DEFAULT_MODE = FolderIteratorMode.COMBINED;

	private final UcmSession session;
	private final FolderLocatorMode folderLocatorMode;
	private final Object searchKey;
	private final int pageSize;
	private final FolderIteratorMode folderIteratorMode;

	private int pageCount = -1;
	private int currentInPage = -1;

	private boolean firstRequestIssued = false;
	private UcmAttributes folder = null;
	private UcmAttributes localData = null;
	private String parentPath = null;

	private DataBinder responseBinder = null;
	private Iterator<DataObject> folders = null;
	private List<Field> folderFields = null;
	private Iterator<DataObject> files = null;
	private List<Field> fileFields = null;

	private UcmAttributes current = null;
	private boolean completed = false;

	public FolderContentsIterator(UcmSession session, String path) {
		this(session, FolderLocatorMode.BY_PATH, path, null, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, String path, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_PATH, path, null, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, String path, int pageSize) {
		this(session, FolderLocatorMode.BY_PATH, path, null, pageSize);
	}

	public FolderContentsIterator(UcmSession session, String path, FolderIteratorMode folderIteratorMode,
		int pageSize) {
		this(session, FolderLocatorMode.BY_PATH, path, folderIteratorMode, pageSize);
	}

	public FolderContentsIterator(UcmSession session, UcmUniqueURI uri) {
		this(session, FolderLocatorMode.BY_GUID, uri, null, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, UcmUniqueURI uri, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_GUID, uri, null, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, UcmUniqueURI uri, int pageSize) {
		this(session, FolderLocatorMode.BY_GUID, uri, null, pageSize);
	}

	public FolderContentsIterator(UcmSession session, UcmUniqueURI uri, FolderIteratorMode folderIteratorMode,
		int pageSize) {
		this(session, FolderLocatorMode.BY_GUID, uri, folderIteratorMode, pageSize);
	}

	public FolderContentsIterator(UcmSession session, URI uri) {
		this(session, FolderLocatorMode.BY_URI, uri, null, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, URI uri, FolderIteratorMode folderIteratorMode) {
		this(session, FolderLocatorMode.BY_URI, uri, null, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public FolderContentsIterator(UcmSession session, URI uri, int pageSize) {
		this(session, FolderLocatorMode.BY_URI, uri, null, pageSize);
	}

	public FolderContentsIterator(UcmSession session, URI uri, FolderIteratorMode folderIteratorMode, int pageSize) {
		this(session, FolderLocatorMode.BY_URI, uri, folderIteratorMode, pageSize);
	}

	FolderContentsIterator(UcmSession session, FolderLocatorMode folderLocatorMode, Object searchKey,
		FolderIteratorMode folderIteratorMode, int pageSize) {
		this.session = Objects.requireNonNull(session, "Must provide a non-null session");
		this.searchKey = folderLocatorMode
			.sanitizeKey(Objects.requireNonNull(searchKey, "Must provide a non-null search criterion"));
		this.folderLocatorMode = folderLocatorMode;
		this.pageSize = Tools.ensureBetween(UcmConstants.MINIMUM_PAGE_SIZE, pageSize, UcmConstants.MAXIMUM_PAGE_SIZE);
		this.folderIteratorMode = Tools.coalesce(folderIteratorMode, FolderContentsIterator.DEFAULT_MODE);
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public UcmSession getSession() {
		return this.session;
	}

	public FolderLocatorMode getSearchMode() {
		return this.folderLocatorMode;
	}

	public Object getSearchKey() {
		return this.searchKey;
	}

	public int getPageCount() {
		return this.pageCount;
	}

	public int getCurrentInPage() {
		return this.currentInPage;
	}

	public int getCurrentPos() {
		return Math.max(-1, (this.pageCount * this.pageSize) + this.currentInPage);
	}

	private DataBinder getNextBatch() throws UcmServiceException {
		try {
			return this.session.callService("FLD_BROWSE", (binder) -> {
				FolderContentsIterator.this.folderLocatorMode.applySearchParameters(binder,
					FolderContentsIterator.this.searchKey);
				FolderContentsIterator.this.folderIteratorMode.setParameters(binder);
				binder.putLocal(FolderContentsIterator.this.folderIteratorMode.count,
					String.valueOf(FolderContentsIterator.this.pageSize));
				binder.putLocal(FolderContentsIterator.this.folderIteratorMode.startRow,
					String.valueOf(FolderContentsIterator.this.pageSize * FolderContentsIterator.this.pageCount));
			}).getResponseAsBinder();
		} catch (IdcClientException e) {
			throw new UcmServiceException(
				String.format("Failed to retrieve page %d for folder [%s]", this.pageCount + 1, this.searchKey), e);
		}
	}

	public UcmAttributes getFolder() throws UcmServiceException {
		if (!this.firstRequestIssued) {
			hasNext();
		}
		return this.folder;
	}

	public UcmAttributes getLocalData() throws UcmServiceException {
		if (!this.firstRequestIssued) {
			hasNext();
		}
		return this.localData;
	}

	public boolean hasNext() throws UcmServiceException {
		if (this.current != null) { return true; }
		if (this.completed) { return false; }

		// Do we need to page? If we do, retrieve the next page...
		if ((this.responseBinder == null) || ((this.currentInPage + 1) >= this.pageSize)) {
			// We need to page...
			this.pageCount++;
			this.responseBinder = getNextBatch();
			this.firstRequestIssued = true;

			if (this.localData == null) {
				DataObject localData = this.responseBinder.getLocalData();
				this.localData = new UcmAttributes(localData, this.responseBinder);
				this.parentPath = Tools.coalesce(localData.get("targetPath"), localData.get("folderPath"));
			}

			if (this.folder == null) {
				DataResultSet rs = this.responseBinder.getResultSet("FolderInfo");
				if (rs != null) {
					List<DataObject> l = rs.getRows();
					if ((l != null) && !l.isEmpty()) {
						UcmAttributes folderAtts = new UcmAttributes(l.get(0), rs.getFields());
						folderAtts.getMutableData().put(UcmAtt.cmfParentPath.name(),
							new CmfValue(FileNameTools.dirname(this.parentPath, '/')));
						this.folder = folderAtts;
					}
				}
			}

			this.currentInPage = -1;
			DataResultSet rs = this.responseBinder.getResultSet("ChildFolders");
			if (rs != null) {
				this.folders = rs.getRows().iterator();
				this.folderFields = rs.getFields();
			} else {
				this.folders = Collections.emptyIterator();
				this.folderFields = Collections.emptyList();
			}

			rs = this.responseBinder.getResultSet("ChildFiles");
			if (rs != null) {
				this.files = rs.getRows().iterator();
				this.fileFields = rs.getFields();
			} else {
				this.files = Collections.emptyIterator();
				this.fileFields = Collections.emptyList();
			}
		}

		if (this.folders.hasNext() || this.files.hasNext()) {
			final boolean folder = this.folders.hasNext();
			DataObject o = (folder ? this.folders.next() : this.files.next());
			Map<String, String> m = new HashMap<>(o);
			m.put(UcmAtt.cmfParentPath.name(), this.parentPath);
			this.current = new UcmAttributes(m, folder ? this.folderFields : this.fileFields);
			this.currentInPage++;
			return true;
		}

		// There are no more elements to be retrieved, so we're done...
		this.completed = true;
		this.responseBinder = null;
		this.current = null;
		this.folders = null;
		this.files = null;

		return false;
	}

	public UcmAttributes next() throws UcmServiceException {
		if (!hasNext()) { throw new NoSuchElementException(); }
		UcmAttributes ret = this.current;
		this.current = null;
		return ret;
	}
}