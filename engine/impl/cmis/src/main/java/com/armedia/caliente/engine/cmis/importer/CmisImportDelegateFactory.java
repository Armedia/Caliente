package com.armedia.caliente.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.dynamic.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportDelegateFactory
	extends ImportDelegateFactory<Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportEngine> {

	CmisImportDelegateFactory(CmisImportEngine engine, Session session, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected CmisImportDelegate<?> newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case FOLDER:
				return new CmisFolderDelegate(this, storedObject);
			case DOCUMENT:
				return new CmisDocumentDelegate(this, storedObject);
			default:
				break;
		}
		return null;
	}

	@Override
	protected CmisSchemaService newSchemaService(Session session) throws SchemaServiceException {
		return new CmisSchemaService(session);
	}
}