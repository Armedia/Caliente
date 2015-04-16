package com.armedia.cmf.engine.documentum.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public abstract class DctmExportDelegate<T extends IDfPersistentObject> extends
ExportDelegate<T, IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportEngine> {

	private final DctmObjectType dctmType;

	protected DctmExportDelegate(DctmExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
		this.dctmType = DctmObjectType.decodeType(this.object);
	}

	@Override
	protected Collection<? extends ExportDelegate<?, IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, ?>> identifyRequirements(
		StoredObject<IDfValue> marshalled, DctmExportContext ctx) throws Exception {
		return new ArrayList<DctmExportDelegate<T>>();
	}

	@Override
	protected Collection<? extends ExportDelegate<?, IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, ?>> identifyDependents(
		StoredObject<IDfValue> marshalled, DctmExportContext ctx) throws Exception {
		return new ArrayList<DctmExportDelegate<T>>();
	}

	@Override
	protected List<ContentInfo> storeContent(IDfSession session, StoredObject<IDfValue> marshalled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		return new ArrayList<ContentInfo>();
	}

	protected final DctmObjectType getDctmType() {
		return this.dctmType;
	}

	@Override
	protected final StoredObjectType calculateType(T object) throws Exception {
		return DctmObjectType.decodeType(object).getStoredObjectType();
	}

	@Override
	protected final String calculateObjectId(T object) throws Exception {
		return object.getObjectId().getId();
	}

	@Override
	protected String calculateLabel(T object) throws Exception {
		return String.format("%s[%s]", getDctmType().name(), getObjectId());
	}

	@Override
	protected String calculateSearchKey(T object) throws Exception {
		return calculateObjectId(object);
	}

	@Override
	protected String calculateBatchId(T object) throws Exception {
		return null;
	}
}