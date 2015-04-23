package com.armedia.cmf.engine.cmis.exporter;

import java.util.List;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;

import com.armedia.cmf.storage.StoredObjectType;

public abstract class CmisFileableDelegate<T extends FileableCmisObject> extends CmisObjectDelegate<T> {

	protected CmisFileableDelegate(CmisExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
	}

	@Override
	protected final String calculateLabel(T obj) throws Exception {
		FileableCmisObject f = FileableCmisObject.class.cast(obj);
		List<String> paths = f.getPaths();
		if (!paths.isEmpty()) { return paths.get(0); }
		return String.format("${unfiled}:%s", f.getName());
	}

	@Override
	protected final StoredObjectType calculateType(T object) throws Exception {
		if (Document.class.isInstance(object)) { return StoredObjectType.DOCUMENT; }
		if (Folder.class.isInstance(object)) { return StoredObjectType.FOLDER; }
		throw new Exception(String.format(
			"Can't identify the type for object with ID [%s] of class [%s] and type [%s]", object.getId(), object
				.getClass().getCanonicalName(), object.getType().getId()));
	}
}