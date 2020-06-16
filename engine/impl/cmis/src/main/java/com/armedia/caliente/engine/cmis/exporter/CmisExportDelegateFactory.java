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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

public class CmisExportDelegateFactory
	extends ExportDelegateFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	final Map<String, Set<String>> pathIdCache = Collections.synchronizedMap(new HashMap<String, Set<String>>());

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
					doc = doc.getObjectOfLatestVersion(false);
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

	@Override
	public void close() {
		this.pathIdCache.clear();
		super.close();
	}
}