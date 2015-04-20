package com.armedia.cmf.engine.cmis.exporter;

import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
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

	@Override
	protected String calculateLabel(T obj) throws Exception {
		if (obj instanceof FileableCmisObject) {
			FileableCmisObject f = FileableCmisObject.class.cast(obj);
			List<String> paths = f.getPaths();
			if (!paths.isEmpty()) { return paths.get(0); }
			return String.format("${unfiled}:%s", f.getName());
		}
		if (obj instanceof CmisObject) {
			CmisObject o = CmisObject.class.cast(obj);
			return String.format("[%s|%s]", o.getType().getId(), o.getName());
		}
		// TODO: Handle other object types...
		return null;
	}
}