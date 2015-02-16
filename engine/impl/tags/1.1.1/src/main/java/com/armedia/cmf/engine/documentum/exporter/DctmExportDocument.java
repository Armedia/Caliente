/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmDocument;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
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

	protected DctmExportDocument(DctmExportEngine engine) {
		super(engine, DctmObjectType.DOCUMENT);
	}

	@Override
	protected String calculateBatchId(IDfSession session, IDfDocument document) throws DfException {
		return document.getChronicleId().getId();
	}

	@Override
	protected void getDataProperties(DctmExportContext ctx, Collection<StoredProperty<IDfValue>> properties,
		IDfDocument document) throws DfException, ExportException {
		super.getDataProperties(ctx, properties, document);

		final IDfSession session = document.getSession();

		if (!isDfReference(document)) {
			// We export our contents...
			StoredProperty<IDfValue> contents = new StoredProperty<IDfValue>(DctmDocument.CONTENTS,
				DctmDataType.DF_ID.getStoredType(), true);
			properties.add(contents);
			String dql = "" //
				+ "select dcs.r_object_id " //
				+ "  from dmr_content_r dcr, dmr_content_s dcs " //
				+ " where dcr.r_object_id = dcs.r_object_id " //
				+ "   and dcr.parent_id = '%s' " //
				+ "   and dcr.page = %d " //
				+ " order by dcs.rendition ";
			final String parentId = document.getObjectId().getId();
			final int pageCount = document.getPageCount();
			for (int i = 0; i < pageCount; i++) {
				IDfCollection results = DfUtils.executeQuery(session, String.format(dql, parentId, i),
					IDfQuery.DF_EXECREAD_QUERY);
				try {
					while (results.next()) {
						contents.addValue(results.getValue(DctmAttributes.R_OBJECT_ID));
					}
				} finally {
					DfUtils.closeQuietly(results);
				}
			}
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

	private List<IDfDocument> getVersions(ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx, boolean prior,
		IDfDocument document) throws ExportException, DfException {
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
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfDocument document, DctmExportContext ctx) throws Exception {
		Collection<IDfPersistentObject> req = super.findRequirements(session, marshaled, document, ctx);

		// Export the ACL
		req.add(document.getACL());

		// We do nothing else for references, as we need nothing else
		if (isDfReference(document)) { return req; }

		// Export the object type
		req.add(document.getType());

		// Export the format
		IDfFormat format = document.getFormat();
		if (format != null) {
			req.add(format);
		}

		// Export the owner
		String owner = DctmMappingUtils.substituteMappableUsers(session, document.getOwnerName());
		if (!DctmMappingUtils.isSubstitutionForMappableUser(owner)) {
			IDfUser user = session.getUser(document.getOwnerName());
			if (user != null) {
				req.add(user);
			}
		}

		// Export the group
		IDfGroup group = session.getGroup(document.getGroupName());
		if (group != null) {
			req.add(group);
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
				req.add(versionDoc);
			}
		}

		// We export our contents...
		for (IDfValue contentId : marshaled.getProperty(DctmDocument.CONTENTS)) {
			IDfPersistentObject content = session.getObject(contentId.asId());
			req.add(content);
		}

		return req;
	}

	@Override
	protected Collection<IDfPersistentObject> findDependents(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfDocument document, ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		// TODO Auto-generated method stub
		Collection<IDfPersistentObject> ret = super.findDependents(session, marshaled, document, ctx);

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
				ret.add(versionDoc);
			}
		}
		return ret;
	}
}