package com.armedia.cmf.engine.cmis.exporter;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;

public class CmisExportContext extends ExportContext<Session, CmfValue> {

	private final CmisExportContextFactory factory;

	CmisExportContext(CmisExportContextFactory factory, String rootId, CmfType rootType, Session session, Logger output) {
		super(factory, factory.getSettings(), rootId, rootType, session, output);
		this.factory = factory;
	}

	public Set<String> convertPermissionToAllowableActions(String permission) {
		return this.factory.convertPermissionToAllowableActions(permission);
	}

	public OperationContext newOperationContext() {
		OperationContext parent = getSession().getDefaultContext();
		OperationContext ctx = getSession().createOperationContext();
		ctx.setCacheEnabled(parent.isCacheEnabled());
		ctx.setFilter(parent.getFilter());
		ctx.setFilterString(parent.getFilterString());
		ctx.setIncludeAcls(parent.isIncludeAcls());
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