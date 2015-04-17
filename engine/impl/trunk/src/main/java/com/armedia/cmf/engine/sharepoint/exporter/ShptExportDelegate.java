package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.independentsoft.share.File;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Group;
import com.independentsoft.share.User;

public abstract class ShptExportDelegate<T> extends
	ExportDelegate<T, ShptSession, ShptSessionWrapper, StoredValue, ShptExportContext, ShptExportEngine> {

	protected ShptExportDelegate(ShptExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
	}

	@Override
	protected Collection<? extends ExportDelegate<?, ShptSession, ShptSessionWrapper, StoredValue, ShptExportContext, ?>> identifyRequirements(
		StoredObject<StoredValue> marshalled, ShptExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected void marshal(ShptExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
	}

	@Override
	protected Collection<? extends ExportDelegate<?, ShptSession, ShptSessionWrapper, StoredValue, ShptExportContext, ?>> identifyDependents(
		StoredObject<StoredValue> marshalled, ShptExportContext ctx) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<ContentInfo> storeContent(ShptSession session, StoredObject<StoredValue> marshalled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected final StoredObjectType calculateType(T object) throws Exception {
		if (object instanceof File) { return StoredObjectType.DOCUMENT; }
		if (object instanceof Folder) { return StoredObjectType.FOLDER; }
		if (object instanceof Group) { return StoredObjectType.GROUP; }
		if (object instanceof User) { return StoredObjectType.USER; }
		return null;
	}

	@Override
	protected String calculateLabel(T object) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String calculateObjectId(T object) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String calculateSearchKey(T object) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}