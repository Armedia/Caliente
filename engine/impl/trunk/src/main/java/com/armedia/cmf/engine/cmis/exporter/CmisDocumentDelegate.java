package com.armedia.cmf.engine.cmis.exporter;

import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;

public class CmisDocumentDelegate extends CmisFileableDelegate<Document> {

	protected CmisDocumentDelegate(CmisExportEngine engine, Document object) throws Exception {
		super(engine, Document.class, object);
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return super.identifyRequirements(marshalled, ctx);
	}

	@Override
	protected void marshal(CmisExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		super.marshal(ctx, object);
	}

	@Override
	protected List<ContentInfo> storeContent(Session session, StoredObject<StoredValue> marshalled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		return super.storeContent(session, marshalled, referrent, streamStore);
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return super.identifyDependents(marshalled, ctx);
	}
}