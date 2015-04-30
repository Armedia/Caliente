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

	public OperationContext getDefaultOperationContext() {
		return getDefaultOperationContext(0);
	}

	public OperationContext getDefaultOperationContext(int itemsPerPage) {
		OperationContext ctx = getSession().createOperationContext();
		ctx.setLoadSecondaryTypeProperties(true);
		ctx.setFilterString("*");
		ctx.setMaxItemsPerPage(itemsPerPage <= 0 ? Integer.MAX_VALUE : itemsPerPage);
		ctx.setIncludePathSegments(true);
		return ctx;
	}
}