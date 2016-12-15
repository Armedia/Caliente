/**
 *
 */

package com.armedia.caliente.engine.documentum.exporter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.activation.MimeType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.documentum.DctmAttributes;
import com.armedia.caliente.engine.documentum.DctmDataType;
import com.armedia.caliente.engine.documentum.DctmVdocMember;
import com.armedia.caliente.engine.documentum.common.DctmDocument;
import com.armedia.caliente.engine.documentum.common.DctmSysObject;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.tools.MimeTools;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfVirtualDocument;
import com.documentum.fc.client.IDfVirtualDocumentNode;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportDocument extends DctmExportSysObject<IDfSysObject> implements DctmDocument {

	protected DctmExportDocument(DctmExportDelegateFactory factory, IDfSysObject document) throws Exception {
		super(factory, IDfSysObject.class, document);
	}

	DctmExportDocument(DctmExportDelegateFactory factory, IDfPersistentObject document) throws Exception {
		this(factory, DctmExportDelegate.staticCast(IDfSysObject.class, document));
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfSysObject document) throws DfException, ExportException {
		if (!super.getDataProperties(ctx, properties, document)) { return false; }

		List<Version<IDfSysObject>> history = getVersionHistory(ctx, document);
		properties.add(new CmfProperty<>(IntermediateProperty.VERSION_COUNT, DctmDataType.DF_INTEGER.getStoredType(),
			false, DfValueFactory.newIntValue(history.size())));
		Integer historyIndex = getVersionIndex(document, ctx);
		if (historyIndex != null) {
			properties.add(new CmfProperty<>(IntermediateProperty.VERSION_INDEX,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.newIntValue(historyIndex)));
			historyIndex = getHeadIndex(document, ctx);
			if (historyIndex != null) {
				properties.add(new CmfProperty<>(IntermediateProperty.VERSION_HEAD_INDEX,
					DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.newIntValue(historyIndex)));
			}
		}

		List<IDfValue> patches = getVersionPatches(document, ctx);
		if ((patches != null) && !patches.isEmpty()) {
			properties.add(new CmfProperty<>(DctmSysObject.VERSION_PATCHES, DctmDataType.DF_STRING.getStoredType(),
				true, patches));
		}
		IDfValue patchAntecedent = getPatchAntecedent(document, ctx);
		if (patchAntecedent != null) {
			properties.add(new CmfProperty<>(DctmSysObject.PATCH_ANTECEDENT, DctmDataType.DF_ID.getStoredType(), false,
				patchAntecedent));
		}

		if (ctx.getSettings().getBoolean(TransferSetting.LATEST_ONLY)) {
			properties.add(new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
				IntermediateProperty.VERSION_TREE_ROOT.type, DfValueFactory.newBooleanValue(true)));
		}

		// If this is a virtual document, we export the document's components first
		if (document.isVirtualDocument() || (document.getLinkCount() > 0)) {
			CmfProperty<IDfValue> p = new CmfProperty<>(IntermediateProperty.VDOC_MEMBER,
				IntermediateProperty.VDOC_MEMBER.type);
			properties.add(p);
			final IDfVirtualDocument vDoc = document.asVirtualDocument("CURRENT", false);
			final IDfVirtualDocumentNode root = vDoc.getRootNode();
			final int members = root.getChildCount();
			for (int i = 0; i < members; i++) {
				final IDfVirtualDocumentNode child = root.getChild(i);
				p.addValue(DfValueFactory.newStringValue(new DctmVdocMember(child).getEncoded()));

			}
		}
		return true;
	}

	private List<IDfSysObject> getVersions(DctmExportContext ctx, boolean prior, IDfSysObject document)
		throws ExportException, DfException {
		if (document == null) { throw new IllegalArgumentException(
			"Must provide a document whose versions to analyze"); }

		final List<IDfSysObject> ret = new LinkedList<>();

		boolean add = prior;
		for (Version<IDfSysObject> version : getVersionHistory(ctx, document)) {
			IDfSysObject doc = version.object;
			final IDfId id = doc.getObjectId();
			if (Tools.equals(id.getId(), document.getObjectId().getId())) {
				// Once we've found the "reference" object in the history, we skip adding it
				// since it will be added explicitly
				if (!prior) {
					// We need to start adding entries now, so we mark the flag that allows us
					// to do that
					add = true;
					continue;
				}

				// If we're looking for prior versions, we need search no more as we've
				// caught up with the present
				break;
			}

			if (add) {
				ret.add(doc);
			}
		}
		return ret;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfSysObject document, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> req = super.findRequirements(session, marshaled, document, ctx);

		// We do nothing else for references, as we need nothing else
		if (isDfReference(document)) { return req; }

		// If this is a virtual document, we export the document's components first
		if (document.isVirtualDocument() || (document.getLinkCount() > 0)) {
			IDfVirtualDocument vDoc = document.asVirtualDocument("CURRENT", false);
			int components = vDoc.getUniqueObjectIdCount();
			for (int i = 0; i < components; i++) {
				req.add(this.factory.newExportDelegate(session.getObject(vDoc.getUniqueObjectId(i))));
			}
		}
		return req;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findAntecedents(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfSysObject document, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> req = super.findAntecedents(session, marshaled, document, ctx);

		// We do nothing else for references, as we need nothing else
		if (isDfReference(document)) { return req; }

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		boolean rootObject = false;
		if (Tools.equals(marshaled.getId(), ctx.getRootObjectId())) {
			// Now, also do the *PREVIOUS* versions... we'll do the later versions as dependents
			int previousCount = 0;
			for (IDfSysObject versionDoc : getVersions(ctx, true, document)) {
				if (this.log.isDebugEnabled()) {
					this.log
						.debug(String.format("Adding prior version [%s]", calculateVersionString(versionDoc, false)));
				}
				req.add(this.factory.newExportDelegate(versionDoc));
				previousCount++;
			}
			rootObject = (previousCount == 0);
		} else {
			// If we're the first object in the version history, we mark ourselves as such.
			for (Version<IDfSysObject> v : getVersionHistory(ctx, document)) {
				rootObject = (Tools.equals(v.object.getObjectId(), document.getObjectId()));
				break;
			}
		}
		marshaled.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
			IntermediateProperty.VERSION_TREE_ROOT.type, DfValueFactory.newBooleanValue(rootObject)));
		return req;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findSuccessors(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfSysObject document, DctmExportContext ctx) throws Exception {
		// TODO Auto-generated method stub
		Collection<DctmExportDelegate<?>> ret = super.findSuccessors(session, marshaled, document, ctx);

		// References need only the ACL as a dependent
		if (isDfReference(document)) { return ret; }

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		if (Tools.equals(marshaled.getId(), ctx.getRootObjectId())) {
			// Now, also do the *SUBSEQUENT* versions...
			for (IDfSysObject versionDoc : getVersions(ctx, false, document)) {
				if (this.log.isDebugEnabled()) {
					this.log.debug(
						String.format("Adding subsequent version [%s]", calculateVersionString(versionDoc, false)));
				}
				ret.add(this.factory.newExportDelegate(versionDoc));
			}
		}
		return ret;
	}

	@Override
	protected List<CmfContentInfo> doStoreContent(DctmExportContext ctx, CmfAttributeTranslator<IDfValue> translator,
		CmfObject<IDfValue> marshaled, ExportTarget referrent, IDfSysObject document,
		CmfContentStore<?, ?, ?> streamStore, boolean includeRenditions) throws Exception {
		if (isDfReference(document)) { return super.doStoreContent(ctx, translator, marshaled, referrent, document,
			streamStore, includeRenditions); }

		// We export our contents...
		final String dql = "" //
			+ "select dcs.r_object_id " //
			+ "  from dmr_content_r dcr, dmr_content_s dcs " //
			+ " where dcr.r_object_id = dcs.r_object_id " //
			+ "   and dcr.parent_id = '%s' " //
			// If we're not including renditions, then we only want rendition #0 since that's the
			// primary content stream.
			+ (includeRenditions ? "" : "   and dcs.rendition = 0 ") //
			+ "   and dcr.page = %d " //
			+ " order by dcs.rendition, dcr.page ";
		final IDfSession session = ctx.getSession();
		final String parentId = document.getObjectId().getId();
		final int pageCount = document.getPageCount();
		List<CmfContentInfo> cmfContentInfo = new ArrayList<>();
		Set<String> processed = new LinkedHashSet<>();
		for (int i = 0; i < pageCount; i++) {
			IDfCollection results = DfUtils.executeQuery(session, String.format(dql, parentId, i),
				IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					final IDfId contentId = results.getId(DctmAttributes.R_OBJECT_ID);
					if (!processed.add(contentId.getId())) {
						ctx.consistencyWarning(
							"Duplicate content node detected for %s [%s](%s) - content ID [%s] is a duplicate",
							marshaled.getType().name(), marshaled.getLabel(), marshaled.getId(), contentId.getId());
						continue;
					}

					final IDfContent content = IDfContent.class.cast(session.getObject(contentId));
					CmfContentInfo info = storeContentStream(session, translator, marshaled, document, content,
						streamStore, ctx.getSettings().getBoolean(TransferSetting.IGNORE_CONTENT));
					cmfContentInfo.add(info);
				}
			} finally {
				DfUtils.closeQuietly(results);
			}
			// If we're not including renditions, we're also not including multiple pages, so we
			// only do the first page.
			if (!includeRenditions) {
				break;
			}
		}
		return cmfContentInfo;
	}

	protected CmfContentInfo storeContentStream(IDfSession session, CmfAttributeTranslator<IDfValue> translator,
		CmfObject<IDfValue> marshaled, IDfSysObject document, IDfContent content, CmfContentStore<?, ?, ?> streamStore,
		boolean skipContent) throws Exception {
		final String contentId = content.getObjectId().getId();
		if (document == null) { throw new Exception(String
			.format("Could not locate the referrent document for which content [%s] was to be exported", contentId)); }

		String format = content.getString(DctmAttributes.FULL_FORMAT);
		int pageNumber = content.getInt(DctmAttributes.PAGE);
		String pageModifier = content.getString(DctmAttributes.PAGE_MODIFIER);
		if (pageModifier == null) {
			pageModifier = "";
		}

		String renditionId = null;
		if (content.getRendition() != 0) {
			renditionId = String.format("%08x.%s", content.getRendition(), format);
		} else {
			renditionId = null;
		}
		CmfContentInfo info = new CmfContentInfo(renditionId, pageNumber, pageModifier);
		IDfId formatId = content.getFormatId();
		MimeType mimeType = MimeTools.DEFAULT_MIME_TYPE;
		if (!formatId.isNull()) {
			IDfFormat formatObj = IDfFormat.class.cast(session.getObject(formatId));
			String ext = FilenameUtils.getExtension(document.getObjectName());
			if (StringUtils.isEmpty(ext)) {
				ext = formatObj.getDOSExtension();
			}
			info.setExtension(ext);
			mimeType = MimeTools.resolveMimeType(formatObj.getMIMEType());
		}
		info.setMimeType(mimeType);
		info.setFileName(document.getObjectName());
		info.setLength(content.getContentSize());

		info.setProperty(DctmAttributes.SET_FILE, content.getString(DctmAttributes.SET_FILE));
		info.setProperty(DctmAttributes.SET_CLIENT, content.getString(DctmAttributes.SET_CLIENT));
		info.setProperty(DctmAttributes.SET_TIME,
			content.getTime(DctmAttributes.SET_TIME).asString(DctmDocument.CONTENT_SET_TIME_PATTERN));
		info.setProperty(DctmAttributes.FULL_FORMAT, content.getString(DctmAttributes.FULL_FORMAT));
		info.setProperty(DctmAttributes.PAGE_MODIFIER, content.getString(DctmAttributes.PAGE_MODIFIER));
		info.setProperty(DctmAttributes.PAGE, content.getString(DctmAttributes.PAGE));
		info.setProperty(DctmAttributes.RENDITION, content.getString(DctmAttributes.RENDITION));

		// CmfStore the content in the filesystem
		CmfContentStore<?, ?, ?>.Handle contentHandle = streamStore.getHandle(translator, marshaled, info);
		if (!skipContent) {
			if (contentHandle.getSourceStore().isSupportsFileAccess()) {
				document.getFileEx2(contentHandle.getFile(true).getAbsolutePath(), format, pageNumber, pageModifier,
					false);
			} else {
				// Doesn't support file-level, so we (sadly) use stream-level transfers
				InputStream in = null;
				try {
					// Don't pull the content until we're sure we can put it somewhere...
					in = document.getContentEx3(format, pageNumber, pageModifier, false);
					contentHandle.setContents(in);
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		}
		return info;
	}
}