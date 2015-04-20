package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.storage.StoredValue;

public abstract class CmisExportDelegate<T> extends
	ExportDelegate<T, Session, CmisSessionWrapper, StoredValue, CmisExportContext, CmisExportEngine> {

	protected CmisExportDelegate(CmisExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
		// TODO Auto-generated constructor stub
	}
}