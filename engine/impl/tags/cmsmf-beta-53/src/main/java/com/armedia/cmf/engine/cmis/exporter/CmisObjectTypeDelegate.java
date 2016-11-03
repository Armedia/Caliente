package com.armedia.cmf.engine.cmis.exporter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;

public class CmisObjectTypeDelegate extends CmisExportDelegate<ObjectType> {

	protected CmisObjectTypeDelegate(CmisExportDelegateFactory factory, ObjectType folder) throws Exception {
		super(factory, ObjectType.class, folder);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		// TODO: For now, do nothing...
		if (ctx != null) { return false; }

		// Don't marshal base types
		if (this.object.isBaseType()) { return false; }

		for (PropertyDefinition<?> p : this.object.getPropertyDefinitions().values()) {
			// TODO: How to encode this information in an "engine-neutral" fashion?
			p.hashCode();
		}
		return true;
	}

	protected int calculateDepth(ObjectType objectType, final Set<String> visited) throws Exception {
		if (objectType == null) { throw new IllegalArgumentException(
			"Must provide a folder whose depth to calculate"); }
		if (!visited.add(objectType.getId())) { throw new IllegalStateException(
			String.format("ObjectType [%s] was visited twice - visited set: %s", objectType.getId(), visited)); }
		try {
			if (objectType.isBaseType()) { return 0; }
			return calculateDepth(objectType.getParentType(), visited) + 1;
		} finally {
			visited.remove(objectType.getId());
		}
	}

	@Override
	protected String calculateBatchId(ObjectType objectType) throws Exception {
		return String.format("%016x", calculateDepth(objectType, new LinkedHashSet<String>()));
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyRequirements(marshalled, ctx);
		ObjectType objectType = this.object.getParentType();
		if (!objectType.isBaseType()) {
			ret.add(new CmisObjectTypeDelegate(this.factory, objectType));
		}
		return ret;
	}

	@Override
	protected CmfType calculateType(ObjectType object) throws Exception {
		return CmfType.TYPE;
	}

	@Override
	protected String calculateLabel(ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateObjectId(ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateSearchKey(ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateName(ObjectType object) throws Exception {
		return object.getId();
	}
}