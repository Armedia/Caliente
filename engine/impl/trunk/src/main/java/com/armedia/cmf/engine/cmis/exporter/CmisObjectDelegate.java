package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.CmisObject;

public abstract class CmisObjectDelegate<T extends CmisObject> extends CmisExportDelegate<T> {

	protected CmisObjectDelegate(CmisExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
	}

	@Override
	protected String calculateLabel(T obj) throws Exception {
		CmisObject o = CmisObject.class.cast(obj);
		return String.format("[%s|%s]", o.getType().getId(), o.getName());
	}

	@Override
	protected final String calculateObjectId(T object) throws Exception {
		return object.getId();
	}

	@Override
	protected final String calculateSearchKey(T object) throws Exception {
		return object.getId();
	}
}