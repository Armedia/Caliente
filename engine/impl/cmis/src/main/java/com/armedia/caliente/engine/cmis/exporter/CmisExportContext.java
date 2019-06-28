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

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class CmisExportContext extends ExportContext<Session, CmfValue, CmisExportContextFactory> {

	private final RepositoryInfo repositoryInfo;

	CmisExportContext(CmisExportContextFactory factory, String rootId, CmfObject.Archetype rootType, Session session,
		Logger output, WarningTracker warningTracker) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, warningTracker);
		session.setDefaultContext(newOperationContext(session));
		this.repositoryInfo = session.getRepositoryInfo();
	}

	public final RepositoryInfo getRepositoryInfo() {
		return this.repositoryInfo;
	}

	public Set<String> convertPermissionToAllowableActions(String permission) {
		return getFactory().convertPermissionToAllowableActions(permission);
	}

	public OperationContext newOperationContext(Session session) {
		OperationContext parent = session.getDefaultContext();
		OperationContext ctx = session.createOperationContext();
		ctx.setCacheEnabled(parent.isCacheEnabled());
		ctx.setFilter(parent.getFilter());
		ctx.setFilterString(parent.getFilterString());
		// Disable ACL retrieval if ACLs aren't supported
		ctx.setIncludeAcls(getFactory().isSupported(CmfObject.Archetype.ACL) && parent.isIncludeAcls());
		ctx.setIncludeAllowableActions(parent.isIncludeAllowableActions());
		ctx.setIncludePathSegments(parent.isIncludePathSegments());
		ctx.setIncludePolicies(parent.isIncludePolicies());
		ctx.setIncludeRelationships(parent.getIncludeRelationships());
		ctx.setLoadSecondaryTypeProperties(parent.loadSecondaryTypeProperties());
		ctx.setMaxItemsPerPage(parent.getMaxItemsPerPage());
		ctx.setOrderBy(parent.getOrderBy());
		ctx.setRenditionFilter(parent.getRenditionFilter());
		ctx.setRenditionFilterString(parent.getRenditionFilterString());
		return ctx;
	}
}