/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.activation.MimeType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmVdocMember;
import com.armedia.caliente.engine.dfc.common.DctmDocument;
import com.armedia.caliente.engine.dfc.common.DctmSysObject;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.tools.MimeTools;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcVersion;
import com.armedia.caliente.tools.dfc.DfcVersionHistory;
import com.armedia.caliente.tools.dfc.DfcVersionNumber;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfVirtualDocument;
import com.documentum.fc.client.IDfVirtualDocumentNode;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.client.impl.ISysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmExportDocument extends DctmExportSysObject<IDfSysObject> implements DctmDocument {

	protected DctmExportDocument(DctmExportDelegateFactory factory, IDfSession session, IDfSysObject document)
		throws Exception {
		super(factory, session, IDfSysObject.class, document);
	}

	DctmExportDocument(DctmExportDelegateFactory factory, IDfSession session, IDfPersistentObject document)
		throws Exception {
		this(factory, session, DctmExportDelegate.staticCast(IDfSysObject.class, document));
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfSysObject document) throws DfException, ExportException {
		if (!super.getDataProperties(ctx, properties, document)) { return false; }

		DfcVersionHistory<IDfSysObject> history = getVersionHistory(ctx, document);
		properties.add(new CmfProperty<>(IntermediateProperty.HEAD_NAME, IntermediateProperty.HEAD_NAME.type,
			DfValueFactory.of(history.getCurrentVersion().getObject().getObjectName())));
		properties.add(new CmfProperty<>(IntermediateProperty.VERSION_COUNT, DctmDataType.DF_INTEGER.getStoredType(),
			false, DfValueFactory.of(history.size())));
		Integer historyIndex = history.getIndexFor(document.getObjectId());
		if (historyIndex != null) {
			properties.add(new CmfProperty<>(IntermediateProperty.VERSION_INDEX,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.of(historyIndex)));
			historyIndex = history.getCurrentIndex();
			if (historyIndex != null) {
				properties.add(new CmfProperty<>(IntermediateProperty.VERSION_HEAD_INDEX,
					DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.of(historyIndex)));
			}
		}

		List<DfcVersionNumber> patches = history.getPatchesFor(document.getObjectId());
		if ((patches != null) && !patches.isEmpty()) {
			List<IDfValue> patchValues = new ArrayList<>(patches.size());
			for (DfcVersionNumber v : patches) {
				patchValues.add(DfValueFactory.of(v.toString()));
			}
			properties.add(new CmfProperty<>(DctmSysObject.VERSION_PATCHES, DctmDataType.DF_STRING.getStoredType(),
				true, patchValues));
		}
		String patchAntecedent = history.getPatchAntecedentFor(document.getObjectId());
		if (patchAntecedent != null) {
			properties.add(new CmfProperty<>(DctmSysObject.PATCH_ANTECEDENT, DctmDataType.DF_ID.getStoredType(), false,
				DfValueFactory.of(patchAntecedent)));
		}

		if (ctx.getSettings().getBoolean(TransferSetting.LATEST_ONLY)) {
			properties.add(new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
				IntermediateProperty.VERSION_TREE_ROOT.type, DfValueFactory.of(true)));
		}

		// If this is a virtual document, we export the document's components first
		if (document.isVirtualDocument() || (document.getLinkCount() > 0)) {
			CmfProperty<IDfValue> p = new CmfProperty<>(IntermediateProperty.VDOC_MEMBER,
				IntermediateProperty.VDOC_MEMBER.type);
			properties.add(p);
			final IDfVirtualDocument vDoc = document.asVirtualDocument(ISysObject.CURRENT_VERSION_LABEL, false);
			final IDfVirtualDocumentNode root = vDoc.getRootNode();
			final int members = root.getChildCount();
			for (int i = 0; i < members; i++) {
				final IDfVirtualDocumentNode child = root.getChild(i);
				p.addValue(DfValueFactory.of(new DctmVdocMember(child).getEncoded()));

			}
		}
		return true;
	}

	private List<IDfSysObject> getVersions(DctmExportContext ctx, boolean prior, IDfSysObject document)
		throws ExportException, DfException {
		if (document == null) {
			throw new IllegalArgumentException("Must provide a document whose versions to analyze");
		}

		final List<IDfSysObject> ret = new LinkedList<>();

		boolean add = prior;
		for (DfcVersion<IDfSysObject> version : getVersionHistory(ctx, document)) {
			IDfSysObject doc = version.getObject();
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
			IDfVirtualDocument vDoc = document.asVirtualDocument(ISysObject.CURRENT_VERSION_LABEL, false);
			int components = vDoc.getUniqueObjectIdCount();
			for (int i = 0; i < components; i++) {
				req.add(this.factory.newExportDelegate(session, session.getObject(vDoc.getUniqueObjectId(i))));
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
					this.log.debug("Adding prior version [{}]", calculateVersionString(versionDoc, false));
				}
				req.add(this.factory.newExportDelegate(session, versionDoc));
				previousCount++;
			}
			rootObject = (previousCount == 0);
		} else {
			// If we're the first object in the version history, we mark ourselves as such.
			rootObject = Tools.equals(document.getObjectId(),
				getVersionHistory(ctx, document).getRootVersion().getId());
		}
		marshaled.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
			IntermediateProperty.VERSION_TREE_ROOT.type, DfValueFactory.of(rootObject)));
		return req;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findSuccessors(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfSysObject document, DctmExportContext ctx) throws Exception {
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
					this.log.debug("Adding subsequent version [{}]", calculateVersionString(versionDoc, false));
				}
				ret.add(this.factory.newExportDelegate(session, versionDoc));
			}
		}
		return ret;
	}

	@Override
	protected List<CmfContentStream> doStoreContent(DctmExportContext ctx, CmfAttributeTranslator<IDfValue> translator,
		CmfObject<IDfValue> marshaled, ExportTarget referrent, IDfSysObject document, CmfContentStore<?, ?> streamStore,
		boolean includeRenditions) throws DfException {
		if (isDfReference(document)) {
			return super.doStoreContent(ctx, translator, marshaled, referrent, document, streamStore,
				includeRenditions);
		}

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
			+ " order by dcs.rendition, dcs.full_format, dcr.page_modifier, dcr.page ";
		final IDfSession session = ctx.getSession();
		final String parentId = document.getObjectId().getId();
		final int pageCount = document.getPageCount();
		Set<String> processed = new LinkedHashSet<>();
		int index = 0;
		final boolean ignoreContent = ctx.getSettings().getBoolean(TransferSetting.IGNORE_CONTENT);
		Collection<Supplier<CmfContentStream>> suppliers = new ArrayList<>(pageCount);
		for (int i = 0; i < pageCount; i++) {
			try (DfcQuery query = new DfcQuery(session, String.format(dql, parentId, i),
				DfcQuery.Type.DF_EXECREAD_QUERY)) {
				while (query.hasNext()) {
					final IDfId contentId = query.next().getId(DctmAttributes.R_OBJECT_ID);
					if (!processed.add(contentId.getId())) {
						ctx.trackWarning(marshaled,
							"Duplicate content node detected for %s [%s](%s) - content ID [%s] is a duplicate",
							marshaled.getType().name(), marshaled.getLabel(), marshaled.getId(), contentId.getId());
						continue;
					}

					final int idx = (index++);
					final IDfContent content;
					try {
						content = IDfContent.class.cast(session.getObject(contentId));
					} catch (DfException e) {
						this.log.error("Failed to retrieve the content stream object # {} (with id = [{}]) for {}", idx,
							contentId, marshaled.getDescription(), e);
						continue;
					}

					suppliers.add(() -> storeContentStream(session, translator, marshaled, document, content, contentId,
						idx, streamStore, ignoreContent));
				}
			}
			// If we're not including renditions, we're also not including multiple pages, so we
			// only do the first page.
			if (!includeRenditions) {
				break;
			}
		}

		// In preparation for tossing this to the background...
		return suppliers.stream().map((s) -> s.get()).filter(Objects::nonNull)
			.collect(Collectors.toCollection(ArrayList::new));
	}

	protected CmfContentStream storeContentStream(IDfSession session, CmfAttributeTranslator<IDfValue> translator,
		CmfObject<IDfValue> marshaled, IDfSysObject document, IDfContent content, IDfId contentId, int index,
		CmfContentStore<?, ?> streamStore, boolean skipContent) {

		if (document == null) {
			this.log.error("Could not locate the actual {} for which content [{}] (# {}) was to be exported",
				marshaled.getDescription(), contentId, index);
			return null;
		}

		CmfContentStream info = null;
		final String format;
		final String objectName = marshaled.getName();

		try {
			format = content.getString(DctmAttributes.FULL_FORMAT);
			final int renditionPage = content.getInt(DctmAttributes.PAGE);
			final String modifier = Tools.coalesce(content.getString(DctmAttributes.PAGE_MODIFIER), "");
			final int renditionNumber = content.getRendition();
			final String renditionId = ((renditionNumber != 0)
				? String.format("%08x.%s", content.getRendition(), format)
				: null);
			info = new CmfContentStream(index, renditionId, renditionPage, modifier);
		} catch (Exception e) {
			this.log.error(
				"Failed to retrieve the base content metadata for the content stream # {} for {} (contentId = {})",
				index, marshaled.getDescription(), contentId, e);
			return null;
		}

		try {
			IDfId formatId = content.getFormatId();
			final MimeType mimeType;
			if (formatId.isNull()) {
				mimeType = MimeTools.DEFAULT_MIME_TYPE;
			} else {
				IDfFormat formatObj = IDfFormat.class.cast(session.getObject(formatId));
				String ext = FilenameUtils.getExtension(objectName);
				if (StringUtils.isEmpty(ext)) {
					ext = formatObj.getDOSExtension();
				}
				info.setExtension(ext);
				mimeType = MimeTools.resolveMimeType(formatObj.getMIMEType());
			}
			info.setMimeType(mimeType);
			info.setFileName(objectName);
			info.setLength(content.getContentSize());

			info.setProperty(DctmAttributes.SET_FILE, content.getString(DctmAttributes.SET_FILE));
			info.setProperty(DctmAttributes.SET_CLIENT, content.getString(DctmAttributes.SET_CLIENT));
			info.setProperty(DctmAttributes.SET_TIME,
				content.getTime(DctmAttributes.SET_TIME).asString(DctmDocument.CONTENT_SET_TIME_PATTERN));
			info.setProperty(DctmAttributes.FULL_FORMAT, content.getString(DctmAttributes.FULL_FORMAT));
			info.setProperty(DctmAttributes.PAGE_MODIFIER, content.getString(DctmAttributes.PAGE_MODIFIER));
			info.setProperty(DctmAttributes.PAGE, content.getString(DctmAttributes.PAGE));
			info.setProperty(DctmAttributes.RENDITION, content.getString(DctmAttributes.RENDITION));
		} catch (Exception e) {
			this.log.error("Failed to extract additional content metadata properties for the content stream {} for {}",
				info, marshaled.getDescription(), e);
		}

		try {
			// CmfStore the content in the filesystem
			CmfContentStore<?, ?>.Handle contentHandle = streamStore.getHandle(translator, marshaled, info);
			if (!skipContent) {
				if (contentHandle.getSourceStore().isSupportsFileAccess()) {
					document.getFileEx2(contentHandle.getFile(true).getAbsolutePath(), format, info.getRenditionPage(),
						info.getModifier(), false);
				} else {
					// Doesn't support file-level, so we (sadly) use stream-level transfers
					try (InputStream in = document.getContentEx3(format, info.getRenditionPage(), info.getModifier(),
						false)) {
						// Don't pull the content until we're sure we can put it somewhere...
						contentHandle.setContents(in);
					}
				}
			}
		} catch (Exception e) {
			this.log.error("Failed to store the content stream {} for {}", info, marshaled.getDescription(), e);
		}
		return info;
	}
}