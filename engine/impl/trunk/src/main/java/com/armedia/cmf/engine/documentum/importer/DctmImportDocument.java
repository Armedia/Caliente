/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import java.io.File;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmVersionNumber;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmDocument;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredAttributeMapper.Mapping;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportDocument extends DctmImportSysObject<IDfDocument> implements DctmDocument {

	private TemporaryPermission antecedentTemporaryPermission = null;
	private TemporaryPermission branchTemporaryPermission = null;

	protected DctmImportDocument(DctmImportEngine engine, StoredObject<IDfValue> storedObject) {
		super(engine, DctmObjectType.DOCUMENT, storedObject);
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
	protected String calculateBatchId(IDfDocument document) throws DfException {
		return document.getChronicleId().getId();
	}

	@Override
	protected IDfDocument locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		final IDfSession session = ctx.getSession();

		if (isReference()) { return locateExistingByPath(ctx); }

		// First things first: are we the root of the version hierarchy?
		StoredAttribute<IDfValue> chronicleAtt = this.storedObject.getAttribute(DctmAttributes.I_CHRONICLE_ID);
		final Mapping chronicleMapping;
		final String implicitLabel;
		if (chronicleAtt != null) {
			String sourceChronicleId = chronicleAtt.getValue().asId().getId();
			implicitLabel = this.storedObject.getAttribute(DctmAttributes.R_VERSION_LABEL).getValue().asString();

			// Map to the new chronicle ID, from the old one...try for the quick win
			chronicleMapping = ctx.getAttributeMapper().getTargetMapping(this.storedObject.getType(),
				DctmAttributes.R_OBJECT_ID, sourceChronicleId);
		} else {
			chronicleMapping = null;
			implicitLabel = null;
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
			if ((existing == null) || (implicitLabel == null)) { return null; }

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
		IDfValue bindingCondition = this.storedObject.getProperty(DctmAttributes.BINDING_CONDITION).getValue();
		IDfValue bindingLabel = this.storedObject.getProperty(DctmAttributes.BINDING_LABEL).getValue();
		IDfValue referenceById = this.storedObject.getProperty(DctmAttributes.REFERENCE_BY_ID).getValue();

		target = session.getObject(referenceById.asId());
		if (!(target instanceof IDfSysObject)) { throw new ImportException(String.format(
			"Reference [%s] target object [%s] is not an IDfSysObject instance", this.storedObject.getLabel(),
			referenceById.asString())); }

		IDfSysObject targetSysObj = IDfSysObject.class.cast(target);
		IDfValue mainFolderAtt = this.storedObject.getProperty(DctmSysObject.TARGET_PARENTS).getValue();
		Mapping m = context.getAttributeMapper().getTargetMapping(DctmObjectType.FOLDER.getStoredObjectType(),
			DctmAttributes.R_OBJECT_ID, mainFolderAtt.asString());
		if (m == null) { throw new ImportException(String.format(
			"Reference [%s] mapping for its parent folder [%s->???] could not be found", this.storedObject.getLabel(),
			mainFolderAtt.asString())); }

		IDfId mainFolderId = new DfId(m.getTargetValue());
		// TODO: Can a reference be *linked* to other folders?
		IDfId newId = targetSysObj.addReference(mainFolderId, bindingCondition.asString(), bindingLabel.asString());
		return castObject(session.getObject(newId));
	}

	@Override
	protected IDfDocument newObject(DctmImportContext context) throws DfException, ImportException {

		if (isReference()) { return newReference(context); }

		final String sourceChronicleId = this.storedObject.getAttribute(DctmAttributes.I_CHRONICLE_ID).getValue()
			.asId().getId();
		final boolean root = (Tools.equals(this.storedObject.getId(), sourceChronicleId));
		if (root) { return super.newObject(context); }

		final IDfSession session = context.getSession();

		final IDfId antecedentId;
		IDfDocument antecedentVersion = null;
		final StoredProperty<IDfValue> antecedentProperty = this.storedObject
			.getProperty(DctmSysObject.PATCH_ANTECEDENT);
		if (antecedentProperty == null) {
			antecedentId = this.storedObject.getAttribute(DctmAttributes.I_ANTECEDENT_ID).getValue().asId();
		} else {
			IDfId aid = antecedentProperty.getValue().asId();
			if (aid.isObjectId()) {
				antecedentId = aid;
			} else {
				antecedentId = null;
				Mapping mapping = context.getAttributeMapper().getTargetMapping(this.storedObject.getType(),
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
				mapping = context.getAttributeMapper().getTargetMapping(this.storedObject.getType(),
					DctmAttributes.R_OBJECT_ID, antecedentId.getId());
			}
			if (mapping == null) {
				// The root of the trunk is missing...we'll need to create a new, contentless
				// object
				antecedentVersion = super.newObject(context);

				// Set the name
				antecedentVersion.setObjectName(this.storedObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue()
					.asString());

				// Set the owner and group
				antecedentVersion.setOwnerName(this.storedObject.getAttribute(DctmAttributes.OWNER_NAME).getValue()
					.asString());
				antecedentVersion.setGroupName(this.storedObject.getAttribute(DctmAttributes.GROUP_NAME).getValue()
					.asString());

				// Set the ACL
				antecedentVersion.setACLDomain(this.storedObject.getAttribute(DctmAttributes.ACL_DOMAIN).getValue()
					.asString());
				antecedentVersion.setACLName(this.storedObject.getAttribute(DctmAttributes.ACL_NAME).getValue()
					.asString());

				// Link to prospective parents
				// TODO: Mess with parents' permissions?
				linkToParents(antecedentVersion, context);

				// Create the chronicle mapping
				// TODO: How do we revert this if the transaction fails later on?
				context.getAttributeMapper().setMapping(this.storedObject.getType(), DctmAttributes.R_OBJECT_ID,
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

		StoredProperty<IDfValue> patches = this.storedObject.getProperty(DctmSysObject.VERSION_PATCHES);
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
				final StoredProperty<IDfValue> prop = new StoredProperty<IDfValue>(DctmAttributes.R_VERSION_LABEL,
					StoredDataType.STRING, false, DfValueFactory.newStringValue(p.toString()));
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
			DctmVersionNumber newVersion = new DctmVersionNumber(this.storedObject
				.getAttribute(DctmAttributes.R_VERSION_LABEL).getValue().asString());
			DctmVersionNumber lastVersion = new DctmVersionNumber(lastAntecedentVersion.asString());
			if (newVersion.isSuccessorOf(lastVersion)) {
				antecedentVersion = lastAntecedent;
			}
		}

		return createSuccessorVersion(antecedentVersion, null, context);
	}

	private IDfDocument createSuccessorVersion(IDfDocument antecedentVersion, StoredProperty<IDfValue> rVersionLabel,
		DctmImportContext context) throws DfException {
		final IDfSession session = antecedentVersion.getSession();
		antecedentVersion.fetch(null);
		this.antecedentTemporaryPermission = new TemporaryPermission(antecedentVersion, IDfACL.DF_PERMIT_DELETE);
		if (this.antecedentTemporaryPermission.grant(antecedentVersion)) {
			antecedentVersion.save();
		}

		// This is so later on we can decide whether we should set or clear the immutable flag,
		// but only if we're a leaf node
		StoredAttribute<IDfValue> descendantCount = this.storedObject.getAttribute(DctmAttributes.I_DIRECT_DSC);
		if ((descendantCount != null) && (descendantCount.getValue().asInteger() == 0)) {
			detectIncomingMutability();
		}

		if (rVersionLabel == null) {
			rVersionLabel = this.storedObject.getAttribute(DctmAttributes.R_VERSION_LABEL);
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
	protected void prepareOperation(IDfDocument sysObject, boolean newObject) throws DfException, ImportException {
		super.prepareOperation(sysObject, newObject);
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
		String sourceChronicleId = this.storedObject.getAttribute(DctmAttributes.I_CHRONICLE_ID).getValue().asId()
			.getId();
		final boolean root = (Tools.equals(this.storedObject.getId(), sourceChronicleId));
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

	@Override
	protected void finalizeConstruction(final IDfDocument document, boolean newObject, final DctmImportContext context)
		throws DfException, ImportException {

		// References don't require any of this being done
		if (isReference()) { return; }

		final StoredObject<IDfValue> storedObject = this.storedObject;
		final IDfSession session = document.getSession();

		// Now, create the content the contents
		Set<String> contentIds = new HashSet<String>();
		StoredProperty<IDfValue> contentProperty = storedObject.getProperty(DctmDocument.CONTENTS);
		if ((contentProperty != null) && (contentProperty.getValueCount() > 0)) {
			for (IDfValue contentId : contentProperty) {
				contentIds.add(contentId.asString());
			}
		}

		final String documentId = document.getObjectId().getId();
		final String contentType = storedObject.getAttribute(DctmAttributes.A_CONTENT_TYPE).getValue().toString();
		final int contentCount = contentIds.size();
		final StoredObjectHandler<IDfValue> handler = new StoredObjectHandler<IDfValue>() {

			private final AtomicInteger current = new AtomicInteger(0);

			@Override
			public boolean newBatch(String batchId) throws StorageException {
				this.current.set(0);
				String msg = String.format("Content addition started for document [%s](%s)", storedObject.getLabel(),
					storedObject.getId());
				DctmImportDocument.this.log.info(msg);
				context.printf("\t%s (%d items)", msg, contentCount);
				return true;
			}

			@Override
			public boolean handleObject(StoredObject<IDfValue> storedObject) throws StorageException {
				// Step one: what's the content's path in the filesystem?
				if (storedObject.getType() != StoredObjectType.CONTENT) { return true; }
				final Handle contentHandle = context.getContentHandle(storedObject);
				final File path = contentHandle.getFile();
				final String absolutePath = path.getAbsolutePath();

				final int pageNumber = storedObject.getAttribute(DctmAttributes.PAGE).getValue().asInteger();
				final StoredAttribute<IDfValue> renditionNumber = storedObject.getAttribute(DctmAttributes.RENDITION);
				final StoredAttribute<IDfValue> pageModifierAtt = storedObject
					.getAttribute(DctmAttributes.PAGE_MODIFIER);
				final StoredAttribute<IDfValue> fullFormatAtt = storedObject.getAttribute(DctmAttributes.FULL_FORMAT);
				final String pageModifier = ((pageModifierAtt != null) && pageModifierAtt.hasValues() ? pageModifierAtt
					.getValue() : DctmDataType.DF_STRING.getNull()).asString();
				String fullFormat = (fullFormatAtt != null ? fullFormatAtt.getValue().asString() : contentType);

				if ((renditionNumber == null) || (renditionNumber.getValue().asInteger() == 0)) {
					if ((fullFormat != null) && !Tools.equals(fullFormat, contentType)) {
						fullFormat = contentType;
					}
					try {
						document.setFileEx(absolutePath, fullFormat, pageNumber, null);
						final String msg = String.format(
							"Added the primary content to document [%s](%s) -> {%s/%s/%s}", storedObject.getLabel(),
							storedObject.getId(), absolutePath, fullFormat, pageNumber);
						DctmImportDocument.this.log.info(msg);
						context.printf("\t%s (item %d of %d)", msg, this.current.incrementAndGet(), contentCount);
					} catch (DfException e) {
						final String msg = String.format(
							"Failed to add the primary content to document [%s](%s) -> {%s/%s/%s}",
							storedObject.getLabel(), storedObject.getId(), absolutePath, fullFormat, pageNumber);
						context.printf("\t%s (item %d of %d): %s [%s]", msg, this.current.incrementAndGet(),
							contentCount, e.getClass().getCanonicalName(), e.getMessage());
						throw new StorageException(msg, e);
					}
				} else {
					try {
						document.addRenditionEx2(absolutePath, fullFormat, pageNumber, pageModifier, null, false,
							false, false);
						final String msg = String.format(
							"Added rendition content to document [%s](%s) -> {%s/%s/%s/%s}", storedObject.getLabel(),
							storedObject.getId(), absolutePath, fullFormat, pageNumber, pageModifier);
						DctmImportDocument.this.log.info(msg);
						context.printf("\t%s (item %d of %d)", msg, this.current.incrementAndGet(), contentCount);
					} catch (DfException e) {
						final String msg = String.format(
							"Failed to add rendition content to document [%s](%s) -> {%s/%s/%s/%s}",
							storedObject.getLabel(), storedObject.getId(), absolutePath, fullFormat, pageNumber,
							pageModifier);
						context.printf("\t%s (item %d of %d): %s [%s]", msg, this.current.incrementAndGet(),
							contentCount, e.getClass().getCanonicalName(), e.getMessage());
						throw new StorageException(msg, e);
					}
				}

				String setFile = storedObject.getAttribute(DctmAttributes.SET_FILE).getValue().asString();
				if (StringUtils.isBlank(setFile)) {
					setFile = " ";
				}
				// If setFile contains single quote in its contents, to escape it, replace it
				// with 4 single quotes.
				setFile = setFile.replaceAll("'", "''''");

				String setClient = storedObject.getAttribute(DctmAttributes.SET_CLIENT).getValue().asString();
				if (StringUtils.isBlank(setClient)) {
					setClient = " ";
				}

				IDfTime setTime = storedObject.getAttribute(DctmAttributes.SET_TIME).getValue().asTime();

				String pageModifierClause = "";
				if (!StringUtils.isBlank(pageModifier)) {
					pageModifierClause = String.format("and dcr.page_modifier = ''%s''", pageModifier);
				}

				// Prepare the sql to be executed
				String sql = "" //
					+ "UPDATE dmr_content_s SET " //
					+ "       set_file = ''%s'', " //
					+ "       set_client = ''%s'', " //
					+ "       set_time = %s " //
					+ " WHERE r_object_id = (" //
					+ "           select dcs.r_object_id " //
					+ "             from dmr_content_s dcs, dmr_content_r dcr " //
					+ "            where dcr.parent_id = ''%s'' " //
					+ "              and dcs.r_object_id = dcr.r_object_id " //
					+ "              and dcs.rendition = %d " //
					+ "              %s " //
					+ "              and dcr.page = %d " //
					+ "              and dcs.full_format = ''%s''" //
					+ "       )";

				try {
					// Run the exec sql
					sql = String.format(sql, setFile, setClient, DfUtils.generateSqlDateClause(setTime.getDate(),
						session), documentId, renditionNumber.getValue().asInteger(), pageModifierClause, pageNumber,
						fullFormat);
					if (!runExecSQL(session, sql)) {
						final String msg = String
							.format(
								"SQL Execution failed for updating the content's system attributes for document [%s](%s) -> {%s/%s/%s/%s}:%n%s%n",
								storedObject.getLabel(), storedObject.getId(), absolutePath, fullFormat, pageNumber,
								pageModifier, sql);
						context.printf("\t%s (item %d of %d)", msg, this.current.incrementAndGet(), contentCount);
						throw new StorageException(msg);
					}
					return true;
				} catch (DfException e) {
					final String msg = String
						.format(
							"Exception caught generating updating the content's system attributes for document [%s](%s) -> {%s/%s/%s/%s}",
							storedObject.getLabel(), storedObject.getId(), absolutePath, fullFormat, pageNumber,
							pageModifier);
					context.printf("\t%s (item %d of %d): %s [%s]", msg, this.current.incrementAndGet(), contentCount,
						e.getClass().getCanonicalName(), e.getMessage());
					throw new StorageException(msg, e);
				}
			}

			@Override
			public boolean closeBatch(boolean ok) throws StorageException {
				String msg = String.format("Content addition finished for document [%s](%s)", storedObject.getLabel(),
					storedObject.getId());
				DctmImportDocument.this.log.info(msg);
				context.printf("\t%s (%d items)", msg, contentCount);
				return true;
			}

			@Override
			public boolean handleException(SQLException e) {
				String msg = String.format("SQLException caught processing content for [%s](%s)",
					storedObject.getLabel(), storedObject.getId());
				DctmImportDocument.this.log.error(msg, e);
				context.printf("\t%s: %s", msg, e.getMessage());
				return true;
			}

		};
		try {
			context.loadObjects(DctmObjectType.CONTENT.getStoredObjectType(), contentIds, handler);
		} catch (Exception e) {
			throw new ImportException(String.format("Exception caught loading content for document [%s](%s)",
				storedObject.getLabel(), storedObject.getId()), e);
		}

		// Now, link to the parent folders
		linkToParents(document, context);
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