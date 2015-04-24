package com.armedia.cmf.engine.cmis.exporter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;

public class CmisDocumentDelegate extends CmisFileableDelegate<Document> {

	protected CmisDocumentDelegate(CmisExportEngine engine, Document object) throws Exception {
		super(engine, Document.class, object);
	}

	@Override
	protected String calculateBatchId(Document object) throws Exception {
		return object.getVersionSeriesId();
	}

	@Override
	protected String calculateVersion(Document obj) throws Exception {
		return obj.getVersionLabel();
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
		List<ContentInfo> ret = super.storeContent(session, marshalled, referrent, streamStore);
		ContentStream main = this.object.getContentStream();
		Handle mainHandle = storeContentStream(marshalled, null, main, streamStore);
		ContentInfo mainInfo = new ContentInfo(mainHandle.getQualifier());
		mainInfo.setProperty("mimeType", main.getMimeType());
		mainInfo.setProperty("size", String.valueOf(main.getLength()));
		mainInfo.setProperty("fileName", main.getFileName());
		ret.add(mainInfo);
		for (Rendition r : this.object.getRenditions()) {
			ContentStream cs = r.getContentStream();
			Handle handle = storeContentStream(marshalled, r, cs, streamStore);
			ContentInfo info = new ContentInfo(handle.getQualifier());
			info.setProperty("kind", r.getKind());
			info.setProperty("mimeType", r.getMimeType());
			info.setProperty("docId", r.getRenditionDocumentId());
			info.setProperty("streamId", r.getStreamId());
			info.setProperty("title", r.getTitle());
			info.setProperty("height", String.valueOf(r.getHeight()));
			info.setProperty("width", String.valueOf(r.getWidth()));
			info.setProperty("size", String.valueOf(cs.getLength()));
			info.setProperty("fileName", cs.getFileName());
			ret.add(info);
		}
		return ret;
	}

	protected Handle storeContentStream(StoredObject<StoredValue> marshalled, Rendition r, ContentStream cs,
		ContentStore streamStore) throws Exception {
		Handle h = streamStore.getHandle(marshalled, r != null ? r.getKind() : "");
		InputStream src = cs.getStream();
		OutputStream tgt = h.openOutput();
		try {
			IOUtils.copy(src, tgt);
		} finally {
			IOUtils.closeQuietly(src);
			IOUtils.closeQuietly(tgt);
		}
		return h;
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return super.identifyDependents(marshalled, ctx);
	}
}