package com.armedia.cmf.engine.cmis.export;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisSessionFactory;
import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;

public class CmisExportEngine extends
	ExportEngine<Session, CmisSessionWrapper, CmisObject, Property<?>, CmisExportContext> {

	public CmisExportEngine() {
	}

	@Override
	protected String getObjectId(CmisObject sourceObject) {
		return sourceObject.getId();
	}

	@Override
	protected String calculateLabel(CmisObject sourceObject) throws Exception {
		return null;
	}

	@Override
	protected Iterator<ExportTarget> findExportResults(Session session, Map<String, ?> settings) throws Exception {
		return null;
	}

	@Override
	protected CmisObject getObject(Session session, StoredObjectType type, String searchKey) throws Exception {
		return null;
	}

	@Override
	protected Collection<CmisObject> identifyRequirements(Session session, StoredObject<Property<?>> marshalled,
		CmisObject object, CmisExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected Collection<CmisObject> identifyDependents(Session session, StoredObject<Property<?>> marshalled,
		CmisObject object, CmisExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected ExportTarget getExportTarget(CmisObject object) throws ExportException {
		return null;
	}

	@Override
	protected StoredObject<Property<?>> marshal(CmisExportContext ctx, Session session, CmisObject object)
		throws ExportException {
		return null;
	}

	@Override
	protected Handle storeContent(Session session, StoredObject<Property<?>> marshalled, ExportTarget referrent,
		CmisObject object, ContentStore streamStore) throws Exception {
		return null;
	}

	@Override
	protected Property<?> getValue(StoredDataType type, Object value) {
		return null;
	}

	@Override
	protected ObjectStorageTranslator<CmisObject, Property<?>> getTranslator() {
		return null;
	}

	@Override
	protected CmisSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return null;
	}

	@Override
	protected CmisExportContextFactory newContextFactory(CfgTools cfg) throws Exception {
		return null;
	}

	@Override
	protected Set<String> getTargetNames() {
		return null;
	}
}