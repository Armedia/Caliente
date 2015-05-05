/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmDocument;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.client.distributed.IDfReference;
import com.documentum.fc.client.distributed.impl.ReferenceFinder;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportDocument extends DctmExportSysObject<IDfDocument> implements DctmDocument {

	private static final String QUALIFIER_FMT = "[%08x]%s.%s";

	protected DctmExportDocument(DctmExportEngine engine, IDfDocument document, CfgTools configuration)
		throws Exception {
		super(engine, IDfDocument.class, document, configuration);
	}

	DctmExportDocument(DctmExportEngine engine, IDfPersistentObject document, CfgTools configuration) throws Exception {
		this(engine, DctmExportDelegate.staticCast(IDfDocument.class, document), configuration);
	}

	@Override
	protected void getDataProperties(DctmExportContext ctx, Collection<StoredProperty<IDfValue>> properties,
		IDfDocument document) throws DfException, ExportException {
		super.getDataProperties(ctx, properties, document);

		final IDfSession session = ctx.getSession();

		if (!isDfReference(document)) {
			getVersionHistory(ctx, document);
			List<IDfValue> patches = getVersionPatches(document, ctx);
			if ((patches != null) && !patches.isEmpty()) {
				properties.add(new StoredProperty<IDfValue>(DctmSysObject.VERSION_PATCHES, DctmDataType.DF_STRING
					.getStoredType(), true, patches));
			}
			IDfValue patchAntecedent = getPatchAntecedent(document, ctx);
			if (patchAntecedent != null) {
				properties.add(new StoredProperty<IDfValue>(DctmSysObject.PATCH_ANTECEDENT, DctmDataType.DF_ID
					.getStoredType(), false, patchAntecedent));
			}

			properties.add(new StoredProperty<IDfValue>(DctmSysObject.CURRENT_VERSION, DctmDataType.DF_BOOLEAN
				.getStoredType(), false, DfValueFactory.newBooleanValue(document.getHasFolder())));
			return;
		}

		// TODO: this is untidy - using an undocumented API??
		IDfReference ref = ReferenceFinder.getForMirrorId(document.getObjectId(), session);
		properties.add(new StoredProperty<IDfValue>(DctmAttributes.BINDING_CONDITION, DctmDataType.DF_STRING
			.getStoredType(), false, DfValueFactory.newStringValue(ref.getBindingCondition())));
		properties.add(new StoredProperty<IDfValue>(DctmAttributes.BINDING_LABEL, DctmDataType.DF_STRING
			.getStoredType(), false, DfValueFactory.newStringValue(ref.getBindingLabel())));
		properties.add(new StoredProperty<IDfValue>(DctmAttributes.LOCAL_FOLDER_LINK, DctmDataType.DF_STRING
			.getStoredType(), false, DfValueFactory.newStringValue(ref.getLocalFolderLink())));
		properties.add(new StoredProperty<IDfValue>(DctmAttributes.REFERENCE_DB_NAME, DctmDataType.DF_STRING
			.getStoredType(), false, DfValueFactory.newStringValue(ref.getReferenceDbName())));
		properties.add(new StoredProperty<IDfValue>(DctmAttributes.REFERENCE_BY_ID, DctmDataType.DF_ID.getStoredType(),
			false, DfValueFactory.newIdValue(ref.getReferenceById())));
		properties.add(new StoredProperty<IDfValue>(DctmAttributes.REFERENCE_BY_NAME, DctmDataType.DF_STRING
			.getStoredType(), false, DfValueFactory.newStringValue(ref.getReferenceByName())));
		properties.add(new StoredProperty<IDfValue>(DctmAttributes.REFRESH_INTERVAL, DctmDataType.DF_INTEGER
			.getStoredType(), false, DfValueFactory.newIntValue(ref.getRefreshInterval())));
	}

	private List<IDfDocument> getVersions(DctmExportContext ctx, boolean prior, IDfDocument document)
		throws ExportException, DfException {
		if (document == null) { throw new IllegalArgumentException("Must provide a document whose versions to analyze"); }

		final List<IDfDocument> ret = new LinkedList<IDfDocument>();

		boolean add = prior;
		for (IDfDocument doc : getVersionHistory(ctx, document)) {
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
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfDocument document, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> req = super.findRequirements(session, marshaled, document, ctx);

		// Export the ACL
		req.add(this.engine.newDelegate(document.getACL(), this.configuration));

		// We do nothing else for references, as we need nothing else
		if (isDfReference(document)) { return req; }

		// Export the object type
		req.add(this.engine.newDelegate(document.getType(), this.configuration));

		// Export the format
		IDfFormat format = document.getFormat();
		if (format != null) {
			req.add(this.engine.newDelegate(format, this.configuration));
		}

		// Export the owner
		String owner = DctmMappingUtils.substituteMappableUsers(session, document.getOwnerName());
		if (!DctmMappingUtils.isSubstitutionForMappableUser(owner)) {
			IDfUser user = session.getUser(document.getOwnerName());
			if (user != null) {
				req.add(this.engine.newDelegate(user, this.configuration));
			}
		}

		// Export the group
		IDfGroup group = session.getGroup(document.getGroupName());
		if (group != null) {
			req.add(this.engine.newDelegate(group, this.configuration));
		}

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		if (Tools.equals(marshaled.getId(), ctx.getRootObjectId())) {
			// Now, also do the *PREVIOUS* versions... we'll do the later versions as dependents
			for (IDfDocument versionDoc : getVersions(ctx, true, document)) {
				if (this.log.isDebugEnabled()) {
					this.log.debug(String
						.format("Adding prior version [%s]", calculateVersionString(versionDoc, false)));
				}
				req.add(this.engine.newDelegate(versionDoc, this.configuration));
			}
		}

		return req;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findDependents(IDfSession session, StoredObject<IDfValue> marshaled,
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
					this.log.debug(String.format("Adding subsequent version [%s]",
						calculateVersionString(versionDoc, false)));
				}
				ret.add(this.engine.newDelegate(versionDoc, this.configuration));
			}
		}
		return ret;
	}

	@Override
	protected List<ContentInfo> doStoreContent(IDfSession session, StoredObject<IDfValue> marshaled,
		ExportTarget referrent, IDfDocument document, ContentStore streamStore) throws Exception {
		if (isDfReference(document)) { return super
			.doStoreContent(session, marshaled, referrent, document, streamStore); }

		// We export our contents...
		String dql = "" //
			+ "select dcs.r_object_id " //
			+ "  from dmr_content_r dcr, dmr_content_s dcs " //
			+ " where dcr.r_object_id = dcs.r_object_id " //
			+ "   and dcr.parent_id = '%s' " //
			+ "   and dcr.page = %d " //
			+ " order by dcs.rendition ";
		final String parentId = document.getObjectId().getId();
		final int pageCount = document.getPageCount();
		List<ContentInfo> contentInfo = new ArrayList<ContentInfo>();
		for (int i = 0; i < pageCount; i++) {
			IDfCollection results = DfUtils.executeQuery(session, String.format(dql, parentId, i),
				IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					final IDfContent content = IDfContent.class.cast(session.getObject(results
						.getId(DctmAttributes.R_OBJECT_ID)));
					Handle handle = storeContentStream(session, marshaled, document, content, streamStore);
					ContentInfo info = new ContentInfo(handle.getQualifier());
					info.setProperty(DctmAttributes.SET_FILE, content.getString(DctmAttributes.SET_FILE));
					info.setProperty(DctmAttributes.SET_CLIENT, content.getString(DctmAttributes.SET_CLIENT));
					info.setProperty(DctmAttributes.SET_TIME,
						content.getTime(DctmAttributes.SET_TIME).asString(DctmDocument.CONTENT_SET_TIME_PATTERN));
					info.setProperty(DctmAttributes.FULL_FORMAT, content.getString(DctmAttributes.FULL_FORMAT));
					info.setProperty(DctmAttributes.PAGE_MODIFIER, content.getString(DctmAttributes.PAGE_MODIFIER));
					info.setProperty(DctmAttributes.PAGE, content.getString(DctmAttributes.PAGE));
					info.setProperty(DctmAttributes.RENDITION, content.getString(DctmAttributes.RENDITION));
					contentInfo.add(info);
				}
			} finally {
				DfUtils.closeQuietly(results);
			}
		}
		return contentInfo;
	}

	protected Handle storeContentStream(IDfSession session, StoredObject<IDfValue> marshaled, IDfDocument document,
		IDfContent content, ContentStore streamStore) throws Exception {
		final String contentId = content.getObjectId().getId();
		if (document == null) { throw new Exception(String.format(
			"Could not locate the referrent document for which content [%s] was to be exported", contentId)); }

		String format = content.getString(DctmAttributes.FULL_FORMAT);
		int pageNumber = content.getInt(DctmAttributes.PAGE);
		String pageModifier = content.getString(DctmAttributes.PAGE_MODIFIER);
		if (pageModifier == null) {
			pageModifier = "";
		}
		String qualifier = String.format(DctmExportDocument.QUALIFIER_FMT, pageNumber, pageModifier, format);

		// Store the content in the filesystem
		Handle contentHandle = streamStore.getHandle(marshaled, qualifier);
		final File targetFile = contentHandle.getFile();
		if (targetFile != null) {
			final File parent = targetFile.getParentFile();
			// Deal with a race condition with multiple threads trying to export to the same folder
			if (!parent.exists()) {
				IOException caught = null;
				for (int i = 0; (i < 3); i++) {
					if (i > 0) {
						// Only sleep if this is a retry
						try {
							Thread.sleep(333);
						} catch (InterruptedException e2) {
							// Ignore...
						}
					}

					try {
						caught = null;
						FileUtils.forceMkdir(parent);
						break;
					} catch (IOException e) {
						// Something went wrong...
						caught = e;
					}
				}
				if (caught != null) { throw new ExportException(String.format(
					"Failed to create the parent content directory [%s]", parent.getAbsolutePath()), caught); }
			}

			if (!parent.isDirectory()) { throw new ExportException(String.format(
				"The parent location [%s] is not a directory", parent.getAbsoluteFile())); }

			document.getFileEx2(targetFile.getAbsolutePath(), format, pageNumber, pageModifier, false);
		} else {
			// Doesn't support file-level, so we (sadly) use stream-level transfers
			InputStream in = null;
			OutputStream out = contentHandle.openOutput();
			try {
				// Don't pull the content until we're sure we can put it somewhere...
				in = document.getContentEx3(format, pageNumber, pageModifier, false);
				IOUtils.copy(in, out);
			} finally {
				IOUtils.closeQuietly(in);
				IOUtils.closeQuietly(out);
			}
		}
		return contentHandle;
	}
}