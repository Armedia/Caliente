package com.armedia.cmf.engine.cmis.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;

public abstract class CmisExportDelegate<T>
extends
ExportDelegate<T, Session, CmisSessionWrapper, StoredValue, CmisExportContext, CmisExportDelegateFactory, CmisExportEngine> {

	protected CmisExportDelegate(CmisExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<CmisExportDelegate<?>>();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<CmisExportDelegate<?>>();
	}

	@Override
	protected List<ContentInfo> storeContent(Session session, ObjectStorageTranslator<StoredValue> translator,
		StoredObject<StoredValue> marshalled, ExportTarget referrent, ContentStore streamStore) throws Exception {
		return new ArrayList<ContentInfo>();
	}
}