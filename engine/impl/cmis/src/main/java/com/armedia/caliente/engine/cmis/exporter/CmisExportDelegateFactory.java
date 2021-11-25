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
package com.armedia.caliente.engine.cmis.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ConcurrentTools;

public class CmisExportDelegateFactory
	extends ExportDelegateFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	public static class CmisHistory {
		public final String historyId;
		private final List<Document> allVersions;
		public final Document firstVersion;
		public final Document lastVersion;
		public final Document lastMajorVersion;
		private final AtomicInteger pendingUsers;

		private CmisHistory(Document d) throws ExportException {
			try {
				this.historyId = d.getVersionSeriesId();
				List<Document> allVersions = d.getAllVersions();

				// First of all: remove the PWC - we're not working with that
				Document first = allVersions.get(0);
				if (first.isPrivateWorkingCopy() == Boolean.TRUE) {
					if ((first.isPrivateWorkingCopy() == Boolean.TRUE)
						|| Objects.equals("pwc", first.getVersionLabel())) {
						allVersions.remove(0);
					}
				}

				this.allVersions = Tools.freezeList(allVersions);
				this.firstVersion = this.allVersions.get(this.allVersions.size() - 1);
				this.lastVersion = this.allVersions.get(0);

				// Avoid another potentially lengthy API call
				Document lastMajorVersion = this.lastVersion;
				for (Document candidate : allVersions) {
					if (candidate.isLatestMajorVersion() == Boolean.TRUE) {
						lastMajorVersion = candidate;
						break;
					}
					lastMajorVersion = null;
				}

				this.lastMajorVersion = lastMajorVersion;
				this.pendingUsers = new AtomicInteger(this.allVersions.size());
			} catch (Exception e) {
				throw new ExportException("Failed to analyze the history for [" + d.getVersionSeriesId() + "]", e);
			}
		}

		public List<Document> getAllVersions() {
			return new ArrayList<>(this.allVersions);
		}
	}

	final ConcurrentMap<String, Set<String>> pathIdCache = new ConcurrentHashMap<>();
	final ConcurrentMap<String, CmisHistory> historyCache = new ConcurrentHashMap<>();

	CmisExportDelegateFactory(CmisExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	protected final <T> T checkedCast(CmisObject obj, Class<T> klazz, CmfObject.Archetype type, String searchKey)
		throws ExportException {
		if (klazz.isInstance(obj)) { return klazz.cast(obj); }
		throw new ExportException(String.format("Object with ID [%s] (class %s) is not a %s-type (%s archetype)",
			searchKey, obj.getClass().getCanonicalName(), klazz.getSimpleName(), type));
	}

	@Override
	protected CmisExportDelegate<?> newExportDelegate(Session session, ExportTarget target) throws Exception {
		CmfObject.Archetype type = target.getType();
		String searchKey = target.getSearchKey();
		CmisObject obj = session.getObject(searchKey);
		switch (type) {
			case FOLDER:
				return new CmisFolderDelegate(this, session, checkedCast(obj, Folder.class, type, searchKey));

			case DOCUMENT:
				// Is this the PWC? If so, then don't include it...
				Document doc = checkedCast(obj, Document.class, type, searchKey);
				if ((doc.isPrivateWorkingCopy() == Boolean.TRUE) || Objects.equals("pwc", doc.getVersionLabel())) {
					// We will not include the PWC in an export
					doc = getHistory(doc).lastVersion;
					if (doc == null) { return null; }
				}
				return new CmisDocumentDelegate(this, session, doc);

			case TYPE:
				ObjectType objectType = checkedCast(obj, ObjectType.class, type, searchKey);
				if (objectType.isBaseType()) { return null; }
				return new CmisObjectTypeDelegate(this, session, objectType);

			case USER:
			case GROUP:
			default:
				break;
		}
		return null;
	}

	protected CmisHistory getHistory(final Document d) throws ExportException {
		return ConcurrentTools.createIfAbsent(this.historyCache, d.getVersionSeriesId(),
			(historyId) -> new CmisHistory(d));
	}

	protected void delegateClosed(CmisExportDelegate<?> delegate) {
		Object object = delegate.getEcmObject();

		// Is this a document delegate? If so, clean out the history cache entry to
		// conserve memory
		if (Document.class.isInstance(object)) {
			final Document doc = Document.class.cast(object);
			final String historyId = doc.getVersionSeriesId();
			final CmisHistory history = this.historyCache.get(historyId);
			if ((history != null) && (history.pendingUsers.decrementAndGet() <= 0)) {
				this.historyCache.remove(historyId);
			}
		}
	}

	@Override
	public void close() {
		this.pathIdCache.clear();
		super.close();
	}
}