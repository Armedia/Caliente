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
package com.armedia.caliente.engine.cmis.exporter;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.cmis.PermissionMapper;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisExportContextFactory
	extends ExportContextFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	private final RepositoryInfo repositoryInfo;
	private final PermissionMapper permissionMapper;

	CmisExportContextFactory(CmisExportEngine engine, Session session, CfgTools settings, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> contentStore, Logger output, WarningTracker warningTracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
		this.repositoryInfo = session.getRepositoryInfo();
		if (super.isSupported(CmfObject.Archetype.ACL)) {
			this.permissionMapper = new PermissionMapper(session);
		} else {
			this.permissionMapper = null;
		}
	}

	public final RepositoryInfo getRepositoryInfo() {
		return this.repositoryInfo;
	}

	public Set<String> convertPermissionToAllowableActions(String permission) {
		if (this.permissionMapper == null) { return null; }
		return this.permissionMapper.convertPermissionToAllowableActions(permission);
	}

	@Override
	protected CmisExportContext constructContext(String rootId, CmfObject.Archetype rootType, Session session,
		int batchPosition) {
		return new CmisExportContext(this, rootId, rootType, session, getOutput(), getWarningTracker());
	}

	@Override
	public final String calculateProductName(Session session) {
		return session.getRepositoryInfo().getProductName();
	}

	@Override
	public final String calculateProductVersion(Session session) {
		return session.getRepositoryInfo().getProductVersion();
	}
}