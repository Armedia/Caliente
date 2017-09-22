package com.armedia.caliente.engine.xds.exporter;

import java.io.InputStream;
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
import org.apache.commons.io.FilenameUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.xds.CmisCustomAttributes;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.MimeTools;
import com.armedia.commons.utilities.Tools;

public class CmisDocumentDelegate extends CmisFileableDelegate<Document> {

	private final String antecedentId;
	private final List<Document> previous;
	private final List<Document> successors;

	protected CmisDocumentDelegate(CmisDocumentDelegate rootElement, Session session, Document object,
		String antecedentId) throws Exception {
		super(rootElement.factory, session, Document.class, object);
		this.previous = Collections.emptyList();
		this.successors = Collections.emptyList();
		this.antecedentId = antecedentId;
	}

	protected CmisDocumentDelegate(CmisExportDelegateFactory factory, Session session, Document object)
		throws Exception {
		super(factory, session, Document.class, object);
		List<Document> all = object.getAllVersions();
		List<Document> prev = new ArrayList<>(all.size());
		List<Document> succ = new ArrayList<>(all.size());

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
	protected String calculateHistoryId(Document object) throws Exception {
		return object.getVersionSeriesId();
	}

	@Override
	protected String calculateVersion(Document obj) throws Exception {
		return obj.getVersionLabel();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyAntecedents(marshalled, ctx);
		String prev = null;
		for (Document d : this.previous) {
			ret.add(new CmisDocumentDelegate(this, ctx.getSession(), d, prev));
			prev = d.getId();
		}
		return ret;
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		if (this.antecedentId != null) {
			CmfAttribute<CmfValue> antecedentId = new CmfAttribute<>(CmisCustomAttributes.VERSION_ANTECEDENT_ID.name,
				CmfDataType.ID, false);
			try {
				antecedentId.setValue(new CmfValue(CmfDataType.ID, Object.class.cast(this.antecedentId)));
			} catch (ParseException e) {
				throw new ExportException(String.format("Failed to create an object ID value for [%s] for %s [%s](%s)",
					this.antecedentId, getType(), getLabel(), getObjectId()));
			}
			object.setAttribute(antecedentId);
		}
		CmfAttribute<CmfValue> current = new CmfAttribute<>(IntermediateAttribute.IS_LATEST_VERSION,
			CmfDataType.BOOLEAN, false);
		current.setValue(new CmfValue(this.object.isLatestVersion()));
		object.setAttribute(current);

		CmfProperty<CmfValue> versionTreeRoot = new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
			CmfDataType.BOOLEAN, false);
		versionTreeRoot.setValue(
			new CmfValue((this.antecedentId == null) || ctx.getSettings().getBoolean(TransferSetting.LATEST_ONLY)));
		object.setProperty(versionTreeRoot);

		if (!this.object.isLatestVersion()) {
			marshalParentsAndPaths(ctx, object, this.object.getObjectOfLatestVersion(false));
		}
		return true;
	}

	@Override
	protected List<CmfContentInfo> storeContent(CmisExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		List<CmfContentInfo> ret = super.storeContent(ctx, translator, marshalled, referrent, streamStore,
			includeRenditions);
		ContentStream main = this.object.getContentStream();
		CmfContentInfo mainInfo = new CmfContentInfo();
		mainInfo.setMimeType(MimeTools.resolveMimeType(main.getMimeType()));
		String name = main.getFileName();
		mainInfo.setFileName(name);
		mainInfo.setExtension(FilenameUtils.getExtension(name));
		long length = storeContentStream(marshalled, translator, null, main, streamStore, mainInfo);
		mainInfo.setLength(length);
		ret.add(mainInfo);
		if (includeRenditions) {
			for (Rendition r : this.object.getRenditions()) {
				CmfContentInfo info = new CmfContentInfo(r.getKind());
				ContentStream cs = r.getContentStream();
				info.setMimeType(MimeTools.resolveMimeType(r.getMimeType()));
				name = cs.getFileName();
				info.setFileName(name);
				info.setExtension(FilenameUtils.getExtension(name));
				info.setProperty("kind", r.getKind());
				info.setProperty("docId", r.getRenditionDocumentId());
				info.setProperty("streamId", r.getStreamId());
				info.setProperty("title", r.getTitle());
				info.setProperty("height", String.valueOf(r.getHeight()));
				info.setProperty("width", String.valueOf(r.getWidth()));
				length = storeContentStream(marshalled, translator, r, cs, streamStore, info);
				info.setLength(length);
				ret.add(info);
			}
		}
		return ret;
	}

	protected long storeContentStream(CmfObject<CmfValue> marshalled, CmfAttributeTranslator<CmfValue> translator,
		Rendition r, ContentStream cs, CmfContentStore<?, ?, ?> streamStore, CmfContentInfo info) throws Exception {
		CmfContentStore<?, ?, ?>.Handle h = streamStore.getHandle(translator, marshalled, info);
		InputStream src = cs.getStream();
		try {
			return h.setContents(src);
		} finally {
			IOUtils.closeQuietly(src);
		}
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifySuccessors(marshalled, ctx);
		String prev = this.object.getId();
		for (Document d : this.successors) {
			ret.add(new CmisDocumentDelegate(this, ctx.getSession(), d, prev));
			prev = d.getId();
		}
		return ret;
	}

	@Override
	protected String calculateName(Document document) throws Exception {
		return document.getName();
	}

	@Override
	protected boolean calculateHistoryCurrent(Document document) throws Exception {
		return document.isLatestVersion();
	}
}