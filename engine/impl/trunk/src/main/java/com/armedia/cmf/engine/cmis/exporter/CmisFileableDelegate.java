package com.armedia.cmf.engine.cmis.exporter;

import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;

import com.armedia.cmf.engine.cmis.CmisAcl;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;

public abstract class CmisFileableDelegate<T extends FileableCmisObject> extends CmisObjectDelegate<T> {

	public static final String MAIN_PATH = "mainPath";

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
	protected void marshal(CmisExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		super.marshal(ctx, object);
		List<String> l = this.object.getPaths();
		if ((l != null) && !l.isEmpty()) {
			StoredProperty<StoredValue> path = new StoredProperty<StoredValue>(CmisFileableDelegate.MAIN_PATH,
				StoredDataType.STRING, new StoredValue(l.get(0)));
			object.setProperty(path);
		}
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyRequirements(marshalled, ctx);
		for (Folder f : this.object.getParents()) {
			ret.add(new CmisFolderDelegate(this.engine, f));
		}
		ret.add(new CmisAclDelegate(this.engine, new CmisAcl(this.engine.decodeType(this.object.getType()), this.object
			.getId(), this.object.getAcl())));
		return ret;
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