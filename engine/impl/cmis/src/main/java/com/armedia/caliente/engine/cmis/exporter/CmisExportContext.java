package com.armedia.caliente.engine.cmis.exporter;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.store.CmfArchetype;
import com.armedia.caliente.store.CmfValue;

public class CmisExportContext extends ExportContext<Session, CmfValue, CmisExportContextFactory> {

	private final RepositoryInfo repositoryInfo;

	CmisExportContext(CmisExportContextFactory factory, String rootId, CmfArchetype rootType, Session session, Logger output,
		WarningTracker warningTracker) {
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
		ctx.setIncludeAcls(getFactory().isSupported(CmfArchetype.ACL) && parent.isIncludeAcls());
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