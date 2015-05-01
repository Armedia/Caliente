package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;

public class CmisExportContext extends ExportContext<Session, StoredValue> {

	CmisExportContext(CmisExportContextFactory factory, String rootId, StoredObjectType rootType, Session session,
		Logger output) {
		super(factory, factory.getSettings(), rootId, rootType, session, output);
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