package com.armedia.cmf.engine.cmis.exporter;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.cmis.CmisAcl;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;

public abstract class CmisFileableDelegate<T extends FileableCmisObject> extends CmisObjectDelegate<T> {

	protected CmisFileableDelegate(CmisExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
	}

	@Override
	protected final String calculateLabel(T f) throws Exception {
		List<String> paths = f.getPaths();
		String ret = null;
		if (!paths.isEmpty()) {
			ret = paths.get(0);
		} else {
			ret = String.format("${unfiled}:%s:%s", f.getName(), f.getId());
		}
		String version = calculateVersion(f);
		if (StringUtils.isBlank(version)) { return ret; }
		return String.format("%s#%s", ret, version);
	}

	protected String calculateVersion(T obj) throws Exception {
		return null;
	}

	@Override
	protected void marshal(CmisExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		super.marshal(ctx, object);
		/*
		List<String> l = this.object.getPaths();
		if ((l != null) && !l.isEmpty()) {
			StoredProperty<StoredValue> path = new StoredProperty<StoredValue>(CmisFileableDelegate.MAIN_PATH,
				StoredDataType.STRING, new StoredValue(l.get(0)));
			object.setProperty(path);
		}
		 */
		StoredProperty<StoredValue> parents = new StoredProperty<StoredValue>(IntermediateProperty.PARENT_ID.encode(),
			StoredDataType.ID, true);
		StoredProperty<StoredValue> paths = new StoredProperty<StoredValue>(IntermediateProperty.PATH.encode(),
			StoredDataType.STRING, true);

		for (Folder f : this.object.getParents()) {
			try {
				parents.addValue(new StoredValue(StoredDataType.ID, f.getId()));
			} catch (ParseException e) {
				// Will not happen...but still
				throw new ExportException(String.format("Failed to store the parent ID [%s] for %s [%s]", f.getId(),
					object.getType(), object.getId()), e);
			}
			for (String p : f.getPaths()) {
				paths.addValue(new StoredValue(p));
			}
		}
		object.setProperty(paths);
		object.setProperty(parents);
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