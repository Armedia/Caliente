/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmVersionNumber;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmDocument;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportSetting;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeMapper.Mapping;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportDocument extends DctmImportSysObject<IDfDocument> implements DctmDocument {

	private static final String DEFAULT_BINARY_MIME = "application/octet-stream";
	private static final String DEFAULT_BINARY_FORMAT = "binary";

	private TemporaryPermission antecedentTemporaryPermission = null;
	private TemporaryPermission branchTemporaryPermission = null;

	protected DctmImportDocument(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfDocument.class, DctmObjectType.DOCUMENT, storedObject);
	}

	private String calculateVersionString(IDfDocument document, boolean full) throws DfException {
		if (!full) { return String.format("%s%s", document.getImplicitVersionLabel(),
			document.getHasFolder() ? ",CURRENT" : ""); }
		int labelCount = document.getVersionLabelCount();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < labelCount; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(document.getVersionLabel(i));
		}
		return sb.toString();
	}

	@Override
	protected String calculateLabel(IDfDocument document) throws DfException, ImportException {
		final int folderCount = document.getFolderIdCount();
		for (int i = 0; i < folderCount; i++) {
			IDfId id = document.getFolderId(i);
			IDfFolder f = IDfFolder.class.cast(document.getSession().getFolderBySpecification(id.getId()));
			if (f != null) {
				String path = (f.getFolderPathCount() > 0 ? f.getFolderPath(0) : String.format("(unknown-folder:[%s])",
					id.getId()));
				return String.format("%s/%s [%s]", path, document.getObjectName(),
					calculateVersionString(document, true));
			}
		}
		throw new ImportException(String.format("None of the parent paths for object [%s] were found", document
			.getObjectId().getId()));
	}

	@Override
	protected IDfDocument locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		final IDfSession session = ctx.getSession();

		if (isReference()) { return locateExistingByPath(ctx); }

		// First things first: are we the root of the version hierarchy?
		CmfAttribute<IDfValue> chronicleAtt = this.cmfObject.getAttribute(DctmAttributes.I_CHRONICLE_ID);
		final Mapping chronicleMapping;
		CmfAttribute<IDfValue> implicitLabelAtt = this.cmfObject.getAttribute(DctmAttributes.R_VERSION_LABEL);
		final String implicitLabel = (implicitLabelAtt != null ? implicitLabelAtt.getValue().asString() : null);
		if (chronicleAtt != null) {
			String sourceChronicleId = chronicleAtt.getValue().asId().getId();
			// Map to the new chronicle ID, from the old one...try for the quick win
			chronicleMapping = ctx.getAttributeMapper().getTargetMapping(this.cmfObject.getType(),
				DctmAttributes.R_OBJECT_ID, sourceChronicleId);
		} else {
			chronicleMapping = null;
		}

		// If we don't have a chronicle mapping, we're likely the root document and thus
		// will have to search by path...
		final String chronicleId;
		IDfDocument existing = null;
		if (chronicleMapping != null) {
			chronicleId = chronicleMapping.getTargetValue();
		} else {
			// We don't know the chronicle, so look by path. We look at all the paths this
			// object is expected to take up on the target, and they must refer to either
			// no existing object, or exactly one existing object. If there is an existing
			// object, it's replaced by this one.
			existing = locateExistingByPath(ctx);

			// If we found no match via path, then we can't locate a match at all and must assume
			// that this object is a new object
			if (implicitLabel == null) { return existing; }
			if (existing == null) { return null; }

			// We have a match, but it may not be the version we seek, so
			// track the chronicle so the code below can find the right version.
			chronicleId = existing.getChronicleId().getId();
		}

		// Using the chronicle ID and the implicit version ID, we will seek out
		// the exact existing version.
		IDfPersistentObject obj = session.getObjectByQualification(String.format(
			"dm_sysobject (all) where i_chronicle_id = '%s' and any r_version_label = %s", chronicleId,
			DfUtils.quoteString(implicitLabel)));

		// Return whatever we found...if we found nothing, then this is a new version
		// and must be handled as such
		return castObject(obj);
	}

	@Override
	protected boolean isVersionable(IDfDocument object) throws DfException {
		// TODO: Are references versionable, per-se?
		return true;
	}

	@Override
	protected IDfId persistChanges(IDfDocument document, DctmImportContext context) throws DfException, ImportException {
		// Apparently, references require no saving
		if (isReference()) { return document.getObjectId(); }
		return super.persistChanges(document, context);
	}

	@Override
	protected boolean isSameObject(IDfDocument object) throws DfException {
		// If we're a reference, and there's something there already, we don't import...
		if (isReference()) { return true; }
		return super.isSameObject(object);
	}

	protected IDfDocument newReference(DctmImportContext context) throws DfException, ImportException {
		IDfPersistentObject target = null;
		IDfSession session = context.getSession();
		IDfValue bindingCondition = this.cmfObject.getProperty(DctmAttributes.BINDING_CONDITION).getValue();
		IDfValue bindingLabel = this.cmfObject.getProperty(DctmAttributes.BINDING_LABEL).getValue();
		IDfValue referenceById = this.cmfObject.getProperty(DctmAttributes.REFERENCE_BY_ID).getValue();

		target = session.getObject(referenceById.asId());
		if (!(target instanceof IDfSysObject)) { throw new ImportException(String.format(
			"Reference [%s] target object [%s] is not an IDfSysObject instance", this.cmfObject.getLabel(),
			referenceById.asString())); }

		IDfSysObject targetSysObj = IDfSysObject.class.cast(target);
		IDfId mainFolderId = getMappedParentId(context);
		if (mainFolderId == null) {
			mainFolderId = this.cmfObject.getProperty(IntermediateProperty.PARENT_ID).getValue().asId();
			throw new ImportException(String.format(
				"Reference [%s] mapping for its parent folder [%s->???] could not be found", this.cmfObject.getLabel(),
				mainFolderId.getId()));
		}
		// TODO: Can a reference be *linked* to other folders?
		IDfId newId = targetSysObj.addReference(mainFolderId, bindingCondition.asString(), bindingLabel.asString());
		return castObject(session.getObject(newId));
	}

	@Override
	protected IDfDocument newObject(DctmImportContext context) throws DfException, ImportException {

		if (isReference()) { return newReference(context); }

		final CmfAttribute<IDfValue> sourceChronicleAtt = this.cmfObject.getAttribute(DctmAttributes.I_CHRONICLE_ID);
		final CmfAttribute<IDfValue> antecedentAtt = this.cmfObject.getAttribute(DctmAttributes.I_ANTECEDENT_ID);
		final String sourceChronicleId = (sourceChronicleAtt != null ? sourceChronicleAtt.getValue().asString() : null);

		// If we have no chronicle info to look for, we don't try to...
		final boolean root = ((sourceChronicleId == null) || (antecedentAtt == null) || Tools.equals(
			this.cmfObject.getId(), sourceChronicleId));
		if (root) { return super.newObject(context); }

		final IDfSession session = context.getSession();

		final IDfId antecedentId;
		IDfDocument antecedentVersion = null;
		final CmfProperty<IDfValue> antecedentProperty = this.cmfObject.getProperty(DctmSysObject.PATCH_ANTECEDENT);
		if (antecedentProperty == null) {
			antecedentId = (antecedentAtt != null ? antecedentAtt.getValue().asId() : DfId.DF_NULLID);
		} else {
			IDfId aid = antecedentProperty.getValue().asId();
			if (aid.isObjectId()) {
				antecedentId = aid;
			} else {
				antecedentId = null;
				Mapping mapping = context.getAttributeMapper().getTargetMapping(this.cmfObject.getType(),
					DctmAttributes.R_OBJECT_ID, sourceChronicleId);
				if (mapping != null) {
					// Find the antecedent using the expected antecedent version number, which
					// by now *should* exist as part of the normal import...
					String dql = String.format(
						"dm_sysobject (ALL) where i_chronicle_id = '%s' and any r_version_label = '%s'",
						mapping.getTargetValue(), antecedentProperty.getValue().asString());
					antecedentVersion = castObject(session.getObjectByQualification(dql));
				}
			}
		}

		if (antecedentVersion == null) {
			Mapping mapping = null;
			if (antecedentId != null) {
				mapping = context.getAttributeMapper().getTargetMapping(this.cmfObject.getType(),
					DctmAttributes.R_OBJECT_ID, antecedentId.getId());
			}
			if (mapping == null) {
				// The root of the trunk is missing...we'll need to create a new, contentless
				// object
				antecedentVersion = super.newObject(context);

				// Set the name
				antecedentVersion.setObjectName(this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue()
					.asString());

				// Link to prospective parents
				// TODO: Mess with parents' permissions?
				linkToParents(antecedentVersion, context);

				// Create the chronicle mapping
				// TODO: How do we revert this if the transaction fails later on?
				context.getAttributeMapper().setMapping(this.cmfObject.getType(), DctmAttributes.R_OBJECT_ID,
					sourceChronicleId, antecedentVersion.getChronicleId().getId());

				// And...finally...
				// TODO: Need a "simple" way to modify the r_modify_date for the document
				updateSystemAttributes(antecedentVersion, context);
			} else {
				IDfId id = new DfId(mapping.getTargetValue());
				session.flushObject(id);
				antecedentVersion = castObject(session.getObject(id));
			}
		}

		CmfProperty<IDfValue> patches = this.cmfObject.getProperty(DctmSysObject.VERSION_PATCHES);
		if ((patches != null) && (patches.getValueCount() > 0)) {
			// Patches are required...so let's carry them out!
			// At the end of patching, antecedentVersion should point to the actual
			// version that will be checked out/branched (i.e. the LAST patch version added)
			// If there is no object, and a root must be created, then do so as well
			IDfDocument lastAntecedent = null;
			IDfValue lastAntecedentVersion = null;
			for (IDfValue p : patches) {
				// Now we checkout and checkin and branch and whatnot as necessary until we can
				// actually proceed with the rest of the algorithm...
				final CmfProperty<IDfValue> prop = new CmfProperty<IDfValue>(DctmAttributes.R_VERSION_LABEL,
					CmfDataType.STRING, false, DfValueFactory.newStringValue(p.toString()));
				IDfDocument patchDocument = createSuccessorVersion(antecedentVersion, prop, context);
				IDfId checkinId = persistNewVersion(patchDocument, p.asString(), context);
				cleanUpTemporaryPermissions(session);
				resetMutabilityFlags();

				// If we branched, we don't change antecedents
				lastAntecedent = castObject(session.getObject(checkinId));
				lastAntecedentVersion = p;
				if (!context.getValue(DctmImportSysObject.BRANCH_MARKER).asBoolean()) {
					antecedentVersion = lastAntecedent;
				}
			}

			// If this version is to be a successor of the last antecedent (all components are the
			// same except the last number), then we go ahead and assign it to antecedentVersion. If
			// it's a sibling (same-level branch), or a descendant (sub-branch), then we leave the
			// antecedent where it is
			DctmVersionNumber newVersion = new DctmVersionNumber(this.cmfObject
				.getAttribute(DctmAttributes.R_VERSION_LABEL).getValue().asString());
			DctmVersionNumber lastVersion = new DctmVersionNumber(lastAntecedentVersion.asString());
			if (newVersion.isSuccessorOf(lastVersion)) {
				antecedentVersion = lastAntecedent;
			}
		}

		return createSuccessorVersion(antecedentVersion, null, context);
	}

	private IDfDocument createSuccessorVersion(IDfDocument antecedentVersion, CmfProperty<IDfValue> rVersionLabel,
		DctmImportContext context) throws ImportException, DfException {
		final IDfSession session = antecedentVersion.getSession();
		antecedentVersion.fetch(null);
		this.antecedentTemporaryPermission = new TemporaryPermission(antecedentVersion, IDfACL.DF_PERMIT_DELETE);
		if (this.antecedentTemporaryPermission.grant(antecedentVersion)) {
			antecedentVersion.save();
		}

		// This is so later on we can decide whether we should set or clear the immutable flag,
		// but only if we're a leaf node
		CmfAttribute<IDfValue> descendantCount = this.cmfObject.getAttribute(DctmAttributes.I_DIRECT_DSC);
		if ((descendantCount != null) && (descendantCount.getValue().asInteger() == 0)) {
			detectIncomingMutability();
		}

		if (rVersionLabel == null) {
			rVersionLabel = this.cmfObject.getAttribute(DctmAttributes.R_VERSION_LABEL);
		}
		String antecedentVersionImplicitVersionLabel = antecedentVersion.getImplicitVersionLabel();
		String documentImplicitVersionLabel = rVersionLabel.getValue().asString();

		int antecedentDots = StringUtils.countMatches(antecedentVersionImplicitVersionLabel, ".");
		int documentDots = StringUtils.countMatches(documentImplicitVersionLabel, ".");

		final boolean shouldBranch = (documentDots == (antecedentDots + 2));
		context.setValue(DctmImportSysObject.BRANCH_MARKER, DfValueFactory.newBooleanValue(shouldBranch));
		if (shouldBranch) {
			// branch
			IDfId branchID = antecedentVersion.branch(antecedentVersionImplicitVersionLabel);
			antecedentVersion = castObject(session.getObject(branchID));
			this.branchTemporaryPermission = new TemporaryPermission(antecedentVersion, IDfACL.DF_PERMIT_DELETE);
			this.branchTemporaryPermission.grant(antecedentVersion);
		} else {
			// checkout
			this.branchTemporaryPermission = null;
			antecedentVersion.checkout();
			antecedentVersion.fetch(null);
		}
		setOwnerGroupACLData(antecedentVersion, context);
		return antecedentVersion;
	}

	private void cleanUpTemporaryPermissions(IDfSession session) throws DfException {
		if (this.antecedentTemporaryPermission != null) {
			IDfId antecedentId = new DfId(this.antecedentTemporaryPermission.getObjectId());
			IDfDocument antecedent = castObject(session.getObject(antecedentId));
			session.flushObject(antecedentId);
			antecedent.fetch(null);
			if (this.antecedentTemporaryPermission.revoke(antecedent)) {
				antecedent.save();
			}
			this.antecedentTemporaryPermission = null;
		}

		if (this.branchTemporaryPermission != null) {
			IDfId branchId = new DfId(this.branchTemporaryPermission.getObjectId());
			IDfDocument branch = castObject(session.getObject(branchId));
			session.flushObject(branchId);
			branch.fetch(null);
			if (this.branchTemporaryPermission.revoke(branch)) {
				branch.save();
			}
			this.branchTemporaryPermission = null;
		}
	}

	@Override
	protected void prepareOperation(IDfDocument sysObject, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		super.prepareOperation(sysObject, newObject, context);
		if (!newObject) {
			// We only do this for old objects because new objects will have been
			// "fixed" in this respect in newObject()
			detectAndClearMutability(sysObject);
		}
	}

	@Override
	protected void prepareForConstruction(IDfDocument document, boolean newObject, DctmImportContext context)
		throws DfException {

		// Is root?
		CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.I_CHRONICLE_ID);
		String sourceChronicleId = (att != null ? att.getValue().asId().getId() : null);
		final boolean root = ((sourceChronicleId == null) || Tools.equals(this.cmfObject.getId(), sourceChronicleId));
		if (!root && !newObject) {
			this.antecedentTemporaryPermission = new TemporaryPermission(document, IDfACL.DF_PERMIT_VERSION);
			if (this.antecedentTemporaryPermission.grant(document)) {
				// Not sure this is OK...
				if (!document.isCheckedOut()) {
					document.save();
				}
			}
		}
	}

	protected boolean saveContentStream(DctmImportContext context, IDfDocument document, CmfContentInfo info,
		CmfContentStore<?, ?, ?>.Handle contentHandle, String contentType, String fullFormat, int pageNumber,
		int renditionNumber, String pageModifier, int currentContent, int totalContentCount) throws DfException,
		ImportException {
		// Step one: what's the content's path in the filesystem?
		final IDfSession session = context.getSession();
		final File path = contentHandle.getFile();
		final String absolutePath = path.getAbsolutePath();

		fullFormat = Tools.coalesce(fullFormat, contentType);

		if (renditionNumber == 0) {
			if (!Tools.equals(fullFormat, contentType)) {
				fullFormat = contentType;
			}
			try {
				document.setFileEx(absolutePath, fullFormat, pageNumber, null);
				final String msg = String.format("Added the primary content to document [%s](%s) -> {%s/%s/%s}",
					this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber);
				DctmImportDocument.this.log.info(msg);
				context.printf("\t%s (item %d of %d)", msg, currentContent, totalContentCount);
			} catch (DfException e) {
				final String msg = String.format(
					"Failed to add the primary content to document [%s](%s) -> {%s/%s/%s}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), absolutePath, fullFormat, pageNumber);
				context.printf("\t%s (item %d of %d): %s [%s]", msg, currentContent, totalContentCount, e.getClass()
					.getCanonicalName(), e.getMessage());
				throw new ImportException(msg, e);
			}
		} else {
			try {
				document.addRenditionEx2(absolutePath, fullFormat, pageNumber, pageModifier, null, false, false, false);
				final String msg = String.format("Added rendition content to document [%s](%s) -> {%s/%s/%s/%s}",
					this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber,
					pageModifier);
				DctmImportDocument.this.log.info(msg);
				context.printf("\t%s (item %d of %d)", msg, currentContent, totalContentCount);
			} catch (DfException e) {
				final String msg = String.format(
					"Failed to add rendition content to document [%s](%s) -> {%s/%s/%s/%s}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), absolutePath, fullFormat, pageNumber, pageModifier);
				context.printf("\t%s (item %d of %d): %s [%s]", msg, currentContent, totalContentCount, e.getClass()
					.getCanonicalName(), e.getMessage());
				throw new ImportException(msg, e);
			}
		}

		String setFile = info.getProperty(DctmAttributes.SET_FILE);
		if (StringUtils.isBlank(setFile)) {
			setFile = " ";
		}

		String setClient = info.getProperty(DctmAttributes.SET_CLIENT);
		if (StringUtils.isBlank(setClient)) {
			setClient = " ";
		}

		String setTimeStr = info.getProperty(DctmAttributes.SET_TIME);
		IDfTime setTime = DfTime.DF_NULLDATE;
		if (setTimeStr != null) {
			setTime = new DfTime(setTimeStr, DctmDocument.CONTENT_SET_TIME_PATTERN);
		}

		if (!StringUtils.isBlank(pageModifier)) {
			pageModifier = DfUtils.sqlQuoteString(pageModifier);
		} else {
			pageModifier = "dcr.page_modifier";
		}

		// Prepare the sql to be executed
		String sql = "" //
			+ "UPDATE dmr_content_s SET " //
			+ "       set_file = %s, " //
			+ "       set_client = %s, " //
			+ "       set_time = %s " //
			+ " WHERE r_object_id = (" //
			+ "           select dcs.r_object_id " //
			+ "             from dmr_content_s dcs, dmr_content_r dcr " //
			+ "            where dcr.parent_id = %s " //
			+ "              and dcs.r_object_id = dcr.r_object_id " //
			+ "              and dcs.rendition = %d " //
			+ "              and dcr.page_modifier = %s " //
			+ "              and dcr.page = %d " //
			+ "              and dcs.full_format = %s" //
			+ "       )";

		if (setTime.isNullDate() || !setTime.isValid()) {
			setTimeStr = "set_time";
		} else {
			setTimeStr = DfUtils.generateSqlDateClause(setTime.getDate(), session);
		}

		final String documentId = document.getObjectId().getId();
		try {
			// Run the exec sql
			sql = String.format(sql, DfUtils.sqlQuoteString(setFile), DfUtils.sqlQuoteString(setClient), setTimeStr,
				DfUtils.sqlQuoteString(documentId), renditionNumber, pageModifier, pageNumber,
				DfUtils.sqlQuoteString(fullFormat));
			if (!runExecSQL(session, sql)) {
				final String msg = String
					.format(
						"SQL Execution failed for updating the content's system attributes for document [%s](%s) -> {%s/%s/%s/%s}:%n%s%n",
						this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber,
						pageModifier, sql);
				context.printf("\t%s (item %d of %d)", msg, currentContent, totalContentCount);
				throw new ImportException(msg);
			}
			return true;
		} catch (DfException e) {
			final String msg = String
				.format(
					"Exception caught generating updating the content's system attributes for document [%s](%s) -> {%s/%s/%s/%s}",
					this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber,
					pageModifier);
			context.printf("\t%s (item %d of %d): %s [%s]", msg, currentContent, totalContentCount, e.getClass()
				.getCanonicalName(), e.getMessage());
			throw new ImportException(msg, e);
		}
	}

	protected String determineFormat(IDfSession session, String contentType) throws DfException {
		String aContentType = contentType;
		if (aContentType == null) {
			CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.A_CONTENT_TYPE);
			if (att == null) { return DctmImportDocument.DEFAULT_BINARY_FORMAT; }
			aContentType = att.getValue().asString();
		}
		if (Tools.equals(aContentType, DctmImportDocument.DEFAULT_BINARY_MIME)) { return DctmImportDocument.DEFAULT_BINARY_FORMAT; }
		IDfFormat format = null;
		try {
			format = session.getFormat(aContentType);
			if (format != null) { return format.getName(); }
		} catch (DfException e) {
			// do nothing... :S
		}

		String dql = "select distinct name from dm_format where mime_type = '%s'";
		try {
			IDfCollection result = DfUtils.executeQuery(session, String.format(dql, aContentType),
				IDfQuery.DF_EXECREAD_QUERY);
			aContentType = DctmImportDocument.DEFAULT_BINARY_FORMAT;
			if (result.next()) {
				aContentType = result.getString("name");
			}
		} catch (DfException e) {
			// Default to a binary file... :S
			aContentType = DctmImportDocument.DEFAULT_BINARY_FORMAT;
		}
		return aContentType;
	}

	protected boolean loadContent(final IDfDocument document, boolean newObject, final DctmImportContext context)
		throws DfException, ImportException {
		List<CmfContentInfo> infoList;
		try {
			infoList = context.getContentInfo(this.cmfObject);
		} catch (Exception e) {
			throw new ImportException(String.format("Failed to load the content info for %s [%s](%s)",
				this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
		CmfContentStore<?, ?, ?> contentStore = context.getContentStore();
		int i = 0;
		final CmfAttribute<IDfValue> contentTypeAtt = this.cmfObject.getAttribute(DctmAttributes.A_CONTENT_TYPE);
		final String contentType = determineFormat(context.getSession(), (contentTypeAtt != null ? contentTypeAtt
			.getValue().toString() : null));
		final CmfAttributeTranslator<IDfValue> translator = this.factory.getEngine().getTranslator();
		for (CmfContentInfo info : infoList) {
			CmfContentStore<?, ?, ?>.Handle h = contentStore.getHandle(translator, this.cmfObject, info.getQualifier());

			CfgTools cfg = info.getCfgTools();
			String fullFormat = cfg.getString(DctmAttributes.FULL_FORMAT);
			int page = cfg.getInteger(DctmAttributes.PAGE, 0);
			String pageModifier = cfg.getString(DctmAttributes.PAGE_MODIFIER, DctmDataType.DF_STRING.getNull()
				.asString());
			int rendition = cfg.getInteger(DctmAttributes.RENDITION, 0);

			saveContentStream(context, document, info, h, contentType, fullFormat, page, rendition, pageModifier, ++i,
				infoList.size());
		}

		return false;
	}

	@Override
	protected void doFinalizeConstruction(final IDfDocument document, boolean newObject, final DctmImportContext context)
		throws DfException, ImportException {

		// References don't require any of this being done
		if (isReference()) { return; }

		if (!context.getSettings().getBoolean(ImportSetting.IGNORE_CONTENT)) {
			loadContent(document, newObject, context);
		}
	}

	@Override
	protected boolean cleanupAfterSave(IDfDocument document, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		final IDfSession session = document.getSession();

		cleanUpParents(session);
		cleanUpTemporaryPermissions(session);

		return super.cleanupAfterSave(document, newObject, context);
	}
}