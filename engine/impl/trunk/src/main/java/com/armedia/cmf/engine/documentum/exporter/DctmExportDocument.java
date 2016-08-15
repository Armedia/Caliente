/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.activation.MimeType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.TransferSetting;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmDocument;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.tools.MimeTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
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
public class DctmExportDocument extends DctmExportSysObject<IDfDocument> implements DctmDocument {

	protected DctmExportDocument(DctmExportDelegateFactory factory, IDfDocument document) throws Exception {
		super(factory, IDfDocument.class, document);
	}

	DctmExportDocument(DctmExportDelegateFactory factory, IDfPersistentObject document) throws Exception {
		this(factory, DctmExportDelegate.staticCast(IDfDocument.class, document));
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfDocument document) throws DfException, ExportException {
		if (!super.getDataProperties(ctx, properties, document)) { return false; }

		List<Version<IDfDocument>> history = getVersionHistory(ctx, document);
		properties.add(new CmfProperty<IDfValue>(IntermediateProperty.VERSION_COUNT,
			DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.newIntValue(history.size())));
		Integer historyIndex = getVersionIndex(document, ctx);
		if (historyIndex != null) {
			properties.add(new CmfProperty<IDfValue>(IntermediateProperty.VERSION_INDEX,
				DctmDataType.DF_INTEGER.getStoredType(), false, DfValueFactory.newIntValue(historyIndex)));
		}

		List<IDfValue> patches = getVersionPatches(document, ctx);
		if ((patches != null) && !patches.isEmpty()) {
			properties.add(new CmfProperty<IDfValue>(DctmSysObject.VERSION_PATCHES,
				DctmDataType.DF_STRING.getStoredType(), true, patches));
		}
		IDfValue patchAntecedent = getPatchAntecedent(document, ctx);
		if (patchAntecedent != null) {
			properties.add(new CmfProperty<IDfValue>(DctmSysObject.PATCH_ANTECEDENT, DctmDataType.DF_ID.getStoredType(),
				false, patchAntecedent));
		}

		// If this is a virtual document, we export the document's components first
		if (document.isVirtualDocument() || (document.getLinkCount() > 0)) {
			CmfProperty<IDfValue> p = new CmfProperty<IDfValue>(IntermediateProperty.VDOC_MEMBER,
				IntermediateProperty.VDOC_MEMBER.type);
			properties.add(p);
			final IDfVirtualDocument vDoc = document.asVirtualDocument("CURRENT", false);
			final IDfVirtualDocumentNode root = vDoc.getRootNode();
			final int members = root.getChildCount();
			for (int i = 0; i < members; i++) {
				final IDfVirtualDocumentNode child = root.getChild(i);
				p.addValue(DfValueFactory.newStringValue(String.format("%s|%s|%s|%s", child.getChronId().getId(),
					child.getBinding(), child.getFollowAssembly(), child.getOverrideLateBindingValue())));
			}
		}
		return true;
	}

	private List<IDfDocument> getVersions(DctmExportContext ctx, boolean prior, IDfDocument document)
		throws ExportException, DfException {
		if (document == null) { throw new IllegalArgumentException(
			"Must provide a document whose versions to analyze"); }

		final List<IDfDocument> ret = new LinkedList<IDfDocument>();

		boolean add = prior;
		for (Version<IDfDocument> version : getVersionHistory(ctx, document)) {
			IDfDocument doc = version.object;
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
		IDfDocument document, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> req = super.findRequirements(session, marshaled, document, ctx);

		// We do nothing else for references, as we need nothing else
		if (isDfReference(document)) { return req; }

		// Export the format
		IDfFormat format = document.getFormat();
		if (format != null) {
			req.add(this.factory.newExportDelegate(format));
		}

		// Export the owner
		String owner = DctmMappingUtils.substituteMappableUsers(session, document.getOwnerName());
		if (!DctmMappingUtils.isSubstitutionForMappableUser(owner)) {
			IDfUser user = session.getUser(document.getOwnerName());
			if (user != null) {
				req.add(this.factory.newExportDelegate(user));
			}
		}

		// Export the group
		IDfGroup group = session.getGroup(document.getGroupName());
		if (group != null) {
			req.add(this.factory.newExportDelegate(group));
		}

		// If this is a virtual document, we export the document's components first
		if (document.isVirtualDocument() || (document.getLinkCount() > 0)) {
			IDfVirtualDocument vDoc = document.asVirtualDocument("CURRENT", false);
			int components = vDoc.getUniqueObjectIdCount();
			for (int i = 0; i < components; i++) {
				req.add(this.factory.newExportDelegate(session.getObject(vDoc.getUniqueObjectId(i))));
			}
		}

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		boolean rootObject = false;
		if (Tools.equals(marshaled.getId(), ctx.getRootObjectId())) {
			// Now, also do the *PREVIOUS* versions... we'll do the later versions as dependents
			int previousCount = 0;
			for (IDfDocument versionDoc : getVersions(ctx, true, document)) {
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
			for (Version<IDfDocument> v : getVersionHistory(ctx, document)) {
				rootObject = (Tools.equals(v.object.getObjectId(), document.getObjectId()));
				break;
			}
		}
		marshaled.setProperty(new CmfProperty<IDfValue>(IntermediateProperty.VERSION_TREE_ROOT,
			IntermediateProperty.VERSION_TREE_ROOT.type, DfValueFactory.newBooleanValue(rootObject)));
		return req;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findDependents(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfDocument document, DctmExportContext ctx) throws Exception {
		// TODO Auto-generated method stub
		Collection<DctmExportDelegate<?>> ret = super.findDependents(session, marshaled, document, ctx);

		// References need only the ACL as a dependent
		if (isDfReference(document)) { return ret; }

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		if (Tools.equals(marshaled.getId(), ctx.getRootObjectId())) {
			// Now, also do the *SUBSEQUENT* versions...
			for (IDfDocument versionDoc : getVersions(ctx, false, document)) {
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
		CmfObject<IDfValue> marshaled, ExportTarget referrent, IDfDocument document,
		CmfContentStore<?, ?, ?> streamStore) throws Exception {
		if (isDfReference(
			document)) { return super.doStoreContent(ctx, translator, marshaled, referrent, document, streamStore); }

		// We export our contents...
		final String dql = "" //
			+ "select dcs.r_object_id " //
			+ "  from dmr_content_r dcr, dmr_content_s dcs " //
			+ " where dcr.r_object_id = dcs.r_object_id " //
			+ "   and dcr.parent_id = '%s' " //
			+ "   and dcr.page = %d " //
			+ " order by dcs.rendition, dcr.page ";
		final IDfSession session = ctx.getSession();
		final String parentId = document.getObjectId().getId();
		final int pageCount = document.getPageCount();
		List<CmfContentInfo> cmfContentInfo = new ArrayList<CmfContentInfo>();
		for (int i = 0; i < pageCount; i++) {
			IDfCollection results = DfUtils.executeQuery(session, String.format(dql, parentId, i),
				IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					final IDfContent content = IDfContent.class
						.cast(session.getObject(results.getId(DctmAttributes.R_OBJECT_ID)));
					CmfContentInfo info = storeContentStream(session, translator, marshaled, document, content,
						streamStore, ctx.getSettings().getBoolean(TransferSetting.IGNORE_CONTENT));
					cmfContentInfo.add(info);
				}
			} finally {
				DfUtils.closeQuietly(results);
			}
		}
		return cmfContentInfo;
	}

	protected CmfContentInfo storeContentStream(IDfSession session, CmfAttributeTranslator<IDfValue> translator,
		CmfObject<IDfValue> marshaled, IDfDocument document, IDfContent content, CmfContentStore<?, ?, ?> streamStore,
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
			renditionId = String.format("%08x", content.getRendition());
		} else {
			renditionId = null;
		}
		CmfContentInfo info = new CmfContentInfo(renditionId, pageNumber);
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