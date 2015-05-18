package com.armedia.cmf.engine.cmis.exporter;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.cmis.CmisCustomAttributes;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.Tools;

public class CmisDocumentDelegate extends CmisFileableDelegate<Document> {

	private final String antecedentId;
	private final List<Document> previous;
	private final List<Document> successors;

	protected CmisDocumentDelegate(CmisDocumentDelegate rootElement, Document object, String antecedentId)
		throws Exception {
		super(rootElement.factory, Document.class, object);
		this.previous = Collections.emptyList();
		this.successors = Collections.emptyList();
		this.antecedentId = antecedentId;
	}

	protected CmisDocumentDelegate(CmisExportDelegateFactory factory, Document object) throws Exception {
		super(factory, Document.class, object);
		List<Document> all = object.getAllVersions();
		List<Document> prev = new ArrayList<Document>(all.size());
		List<Document> succ = new ArrayList<Document>(all.size());

		Document first = all.get(0);
		if ((first.isPrivateWorkingCopy() == Boolean.TRUE) || Tools.equals("pwc", first.getVersionLabel())) {
			all.remove(0);
		}
		Collections.reverse(all);
		List<Document> tgt = prev;
		for (Document d : all) {
			if (Tools.equals(object.getId(), d.getId())) {
				tgt = succ;
				continue;
			}
			tgt.add(d);
		}
		this.previous = Tools.freezeList(prev);
		this.successors = Tools.freezeList(succ);
		this.antecedentId = (prev.isEmpty() ? null : prev.get(prev.size() - 1).getId());
	}

	@Override
	protected String calculatePath(Document d) throws Exception {
		String path = super.calculatePath(d);
		if ((path == null) && !d.isLatestVersion()) {
			path = calculatePath(d.getObjectOfLatestVersion(false));
		}
		return path;
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
		Collection<CmisExportDelegate<?>> ret = super.identifyRequirements(marshalled, ctx);
		String prev = null;
		for (Document d : this.previous) {
			ret.add(new CmisDocumentDelegate(this, d, prev));
			prev = d.getId();
		}
		return ret;
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		if (this.antecedentId != null) {
			StoredAttribute<StoredValue> antecedentId = new StoredAttribute<StoredValue>(
				CmisCustomAttributes.VERSION_ANTECEDENT_ID.name, StoredDataType.ID, false);
			try {
				antecedentId.setValue(new StoredValue(StoredDataType.ID, Object.class.cast(this.antecedentId)));
			} catch (ParseException e) {
				throw new ExportException(String.format("Failed to create an object ID value for [%s] for %s [%s](%s)",
					this.antecedentId, getType(), getLabel(), getObjectId()));
			}
			object.setAttribute(antecedentId);
		}
		StoredProperty<StoredValue> current = new StoredProperty<StoredValue>(
			IntermediateProperty.IS_LATEST_VERSION.encode(), StoredDataType.BOOLEAN, false);
		current.setValue(new StoredValue(this.object.isLatestVersion()));
		object.setProperty(current);

		if (!this.object.isLatestVersion()) {
			marshalParentsAndPaths(ctx, object, this.object.getObjectOfLatestVersion(false));
		}
		return true;
	}

	@Override
	protected List<ContentInfo> storeContent(Session session, ObjectStorageTranslator<StoredValue> translator,
		StoredObject<StoredValue> marshalled, ExportTarget referrent, ContentStore<?> streamStore) throws Exception {
		List<ContentInfo> ret = super.storeContent(session, translator, marshalled, referrent, streamStore);
		ContentStream main = this.object.getContentStream();
		ContentStore<?>.Handle mainHandle = storeContentStream(marshalled, translator, null, main, streamStore);
		ContentInfo mainInfo = new ContentInfo(mainHandle.getQualifier());
		mainInfo.setProperty("mimeType", main.getMimeType());
		mainInfo.setProperty("size", String.valueOf(main.getLength()));
		mainInfo.setProperty("fileName", main.getFileName());
		ret.add(mainInfo);
		for (Rendition r : this.object.getRenditions()) {
			ContentStream cs = r.getContentStream();
			ContentStore<?>.Handle handle = storeContentStream(marshalled, translator, r, cs, streamStore);
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

	protected ContentStore<?>.Handle storeContentStream(StoredObject<StoredValue> marshalled,
		ObjectStorageTranslator<StoredValue> translator, Rendition r, ContentStream cs, ContentStore<?> streamStore)
			throws Exception {
		ContentStore<?>.Handle h = streamStore.getHandle(translator, marshalled, r != null ? r.getKind() : "");
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
		Collection<CmisExportDelegate<?>> ret = super.identifyDependents(marshalled, ctx);
		String prev = this.object.getId();
		for (Document d : this.successors) {
			ret.add(new CmisDocumentDelegate(this, d, prev));
			prev = d.getId();
		}
		return ret;
	}
}