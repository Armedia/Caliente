package com.armedia.cmf.engine.cmis.exporter;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;

public abstract class CmisFileableDelegate<T extends FileableCmisObject> extends CmisObjectDelegate<T> {

	protected CmisFileableDelegate(CmisExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	protected String calculatePath(T f) throws Exception {
		List<String> paths = f.getPaths();
		if (paths.isEmpty()) { return null; }
		return paths.get(0);
	}

	@Override
	protected final String calculateLabel(T f) throws Exception {
		String path = calculatePath(f);
		if (path == null) {
			path = String.format("${unfiled}:%s:%s", f.getName(), f.getId());
		}
		String version = calculateVersion(f);
		if (StringUtils.isBlank(version)) { return path; }
		return String.format("%s#%s", path, version);
	}

	protected String calculateVersion(T obj) throws Exception {
		return null;
	}

	protected void marshalParentsAndPaths(CmisExportContext ctx, CmfObject<CmfValue> marshaled, T object)
		throws ExportException {
		CmfProperty<CmfValue> parents = new CmfProperty<CmfValue>(IntermediateProperty.PARENT_ID, CmfDataType.ID, true);
		CmfProperty<CmfValue> paths = new CmfProperty<CmfValue>(IntermediateProperty.PATH, CmfDataType.STRING, true);
		final String rootPath = ctx.getSession().getRootFolder().getName();
		for (Folder f : object.getParents()) {
			try {
				parents.addValue(new CmfValue(CmfDataType.ID, f.getId()));
			} catch (ParseException e) {
				// Will not happen...but still
				throw new ExportException(String.format("Failed to store the parent ID [%s] for %s [%s]", f.getId(),
					this.object.getType(), this.object.getId()), e);
			}
			for (String p : f.getPaths()) {
				paths.addValue(new CmfValue(String.format("/%s%s", rootPath, p)));
			}
		}
		marshaled.setProperty(paths);
		marshaled.setProperty(parents);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		marshalParentsAndPaths(ctx, object, this.object);
		return true;
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyRequirements(marshalled, ctx);
		for (Folder f : this.object.getParents()) {
			ret.add(new CmisFolderDelegate(this.factory, f));
		}
		ret.add(new CmisAclDelegate(this.factory, this.object));
		ret.add(new CmisObjectTypeDelegate(this.factory, this.object.getType()));
		return ret;
	}

	@Override
	protected final CmfType calculateType(T object) throws Exception {
		if (Document.class.isInstance(object)) { return CmfType.DOCUMENT; }
		if (Folder.class.isInstance(object)) { return CmfType.FOLDER; }
		throw new Exception(String.format(
			"Can't identify the type for object with ID [%s] of class [%s] and type [%s]", object.getId(), object
				.getClass().getCanonicalName(), object.getType().getId()));
	}
}