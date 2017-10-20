package com.armedia.caliente.engine.ucm.exporter;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.io.IOUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.model.UcmAtt;
import com.armedia.caliente.engine.ucm.model.UcmException;
import com.armedia.caliente.engine.ucm.model.UcmFile;
import com.armedia.caliente.engine.ucm.model.UcmFileHistory;
import com.armedia.caliente.engine.ucm.model.UcmRenditionInfo;
import com.armedia.caliente.engine.ucm.model.UcmRevision;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

public class UcmFileExportDelegate extends UcmFSObjectExportDelegate<UcmFile> {

	protected UcmFileExportDelegate(UcmExportDelegateFactory factory, UcmSession session, UcmFile object)
		throws Exception {
		super(factory, session, UcmFile.class, object);
	}

	@Override
	protected String calculateLabel(UcmSession session, UcmFile object) throws Exception {
		return String.format("%s (rev.%s)", super.calculateLabel(session, object), object.getRevisionLabel());
	}

	@Override
	protected boolean calculateHistoryCurrent(UcmSession session, UcmFile object) throws Exception {
		return object.isLatestRevision();
	}

	@Override
	protected int calculateDependencyTier(UcmSession session, UcmFile object) throws Exception {
		return object.isShortcut() ? 1 : 0;
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		UcmExportContext ctx) throws Exception {
		return super.identifyRequirements(marshalled, ctx);
	}

	private UcmFileHistory getHistory(UcmExportContext ctx) throws ExportException {
		final String key = String.format("HISTORY[%s]", this.object.getURI().toString());
		Object o = ctx.getObject(key);
		if (o == null) {
			UcmFileHistory history = null;
			try {
				history = ctx.getSession().getFileHistory(this.object);
				ctx.setObject(key, history);
				o = history;
			} catch (UcmException e) {
				throw new ExportException(e.getMessage(), e);
			}
		}
		if (!UcmFileHistory.class.isInstance(o)) { throw new ExportException(String
			.format("Status violation - history with key %s is of type %s", key, o.getClass().getCanonicalName())); }
		return UcmFileHistory.class.cast(o);
	}

	@Override
	protected boolean marshal(UcmExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }

		UcmFileHistory history = getHistory(ctx);
		boolean latest = (history.getLastRevision().getRevisionId() == this.object.getRevisionNumber());
		CmfAttribute<CmfValue> latestVersion = new CmfAttribute<>(UcmAtt.cmfLatestVersion.name(), CmfDataType.BOOLEAN,
			false, Collections.singleton(new CmfValue(latest)));
		object.setAttribute(latestVersion);

		return true;
	}

	@Override
	protected boolean getDataProperties(UcmExportContext ctx, Collection<CmfProperty<CmfValue>> properties,
		UcmFile object) throws ExportException {
		if (!super.getDataProperties(ctx, properties, object)) { return false; }
		CmfProperty<CmfValue> p = null;

		UcmFileHistory history = getHistory(ctx);

		p = new CmfProperty<>(IntermediateProperty.IS_NEWEST_VERSION, CmfDataType.BOOLEAN,
			new CmfValue(object.isLatestRevision()));
		properties.add(p);

		p = new CmfProperty<>(IntermediateProperty.VERSION_COUNT, IntermediateProperty.VERSION_COUNT.type,
			new CmfValue(history.getRevisionCount()));
		properties.add(p);

		p = new CmfProperty<>(IntermediateProperty.VERSION_INDEX, IntermediateProperty.VERSION_INDEX.type,
			new CmfValue(this.object.getRevisionNumber()));
		properties.add(p);

		p = new CmfProperty<>(IntermediateProperty.VERSION_HEAD_INDEX, IntermediateProperty.VERSION_HEAD_INDEX.type,
			new CmfValue(history.getLastRevision().getRevisionId()));
		properties.add(p);

		return true;
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		Collection<UcmExportDelegate<?>> antecedents = super.identifyAntecedents(marshalled, ctx);
		// Harvest all revisions until we reach this one, then stop harvesting altogether
		for (UcmRevision r : getHistory(ctx)) {
			if (r.getRevisionId() == this.object.getRevisionNumber()) {
				break;
			}
			antecedents
				.add(new UcmFileExportDelegate(this.factory, ctx.getSession(), ctx.getSession().getFileRevision(r)));
		}
		return antecedents;
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		Collection<UcmExportDelegate<?>> successors = super.identifySuccessors(marshalled, ctx);
		boolean harvest = false;
		// Skip all revisions until we find this one, then harvest whatever remains afterwards
		for (UcmRevision r : getHistory(ctx)) {
			if (r.getRevisionId() == this.object.getRevisionNumber()) {
				harvest = true;
				continue;
			}
			if (harvest) {
				successors.add(
					new UcmFileExportDelegate(this.factory, ctx.getSession(), ctx.getSession().getFileRevision(r)));
			}
		}
		return successors;
	}

	@Override
	protected List<CmfContentInfo> storeContent(UcmExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		List<CmfContentInfo> contents = super.storeContent(ctx, translator, marshalled, referrent, streamStore,
			includeRenditions);
		final boolean skipContent = ctx.getSettings().getBoolean(TransferSetting.IGNORE_CONTENT);
		final Map<String, UcmRenditionInfo> renditions = ctx.getSession().getRenditions(this.object);
		for (String r : renditions.keySet()) {
			if (!UcmRenditionInfo.PRIMARY.equalsIgnoreCase(r) && !includeRenditions) {
				// If this isn't the primary renditon and we're not interested in pulling
				// renditions, then we skip this particular rendition
				continue;
			}
			UcmRenditionInfo rendition = renditions.get(r);
			r = r.toUpperCase();

			if (UcmRenditionInfo.PRIMARY.equalsIgnoreCase(r)) {
				r = CmfContentInfo.DEFAULT_RENDITION;
			}

			CmfContentInfo info = new CmfContentInfo(r, 0);
			try {
				info.setMimeType(new MimeType(rendition.getFormat()));
			} catch (MimeTypeParseException e) {
				// Not a problem, really...
				info.setProperty("format", rendition.getFormat());
			}
			info.setProperty("description", rendition.getDescription());

			contents.add(info);
			CmfContentStore<?, ?, ?>.Handle contentHandle = streamStore.getHandle(translator, marshalled, info);
			if (!skipContent) {
				// Doesn't support file-level, so we (sadly) use stream-level transfers
				InputStream in = null;
				try {
					// Don't pull the content until we're sure we can put it somewhere...
					contentHandle.setContents(this.object.getInputStream(ctx.getSession()));
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		}
		return contents;
	}
}