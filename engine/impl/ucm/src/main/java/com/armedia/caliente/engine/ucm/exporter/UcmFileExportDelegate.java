package com.armedia.caliente.engine.ucm.exporter;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.io.IOUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.model.UcmFile;
import com.armedia.caliente.engine.ucm.model.UcmRenditionInfo;
import com.armedia.caliente.engine.ucm.model.UcmRevision;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class UcmFileExportDelegate extends UcmFSObjectExportDelegate<UcmFile> {

	protected UcmFileExportDelegate(UcmExportDelegateFactory factory, UcmFile object) throws Exception {
		super(factory, UcmFile.class, object);
	}

	@Override
	protected String calculateLabel(UcmFile object) throws Exception {
		return String.format("%s (rev.%s)", super.calculateLabel(object), object.getRevisionLabel());
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		UcmExportContext ctx) throws Exception {
		return super.identifyRequirements(marshalled, ctx);
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		Collection<UcmExportDelegate<?>> antecedents = super.identifyAntecedents(marshalled, ctx);
		// Harvest all revisions until we reach this one, then stop harvesting altogether
		for (UcmRevision r : ctx.getSession().getFileHistory(this.object)) {
			if (r.getRevisionId() == this.object.getRevisionNumber()) {
				break;
			}
			antecedents.add(new UcmFileExportDelegate(this.factory, ctx.getSession().getFileRevision(r)));
		}
		return antecedents;
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		Collection<UcmExportDelegate<?>> successors = super.identifySuccessors(marshalled, ctx);
		boolean harvest = false;
		// Skip all revisions until we find this one, then harvest whatever remains afterwards
		for (UcmRevision r : ctx.getSession().getFileHistory(this.object)) {
			if (r.getRevisionId() == this.object.getRevisionNumber()) {
				harvest = true;
				continue;
			}
			if (harvest) {
				successors.add(new UcmFileExportDelegate(this.factory, ctx.getSession().getFileRevision(r)));
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