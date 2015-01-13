/**
 *
 */

package com.delta.cmsmf.cms;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cms.CmsAttributeMapper.Mapping;
import com.delta.cmsmf.cms.storage.CmsObjectStore.ObjectHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.delta.cmsmf.utils.DfVersionNumber;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.distributed.IDfReference;
import com.documentum.fc.client.distributed.impl.ReferenceFinder;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsDocument extends CmsSysObject<IDfDocument> {

	private static final String CONTENTS = "contents";

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsDocument.HANDLERS_READY) { return; }
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID, CmsAttributes.I_FOLDER_ID,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.I_ANTECEDENT_ID, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.I_CHRONICLE_ID, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_STRING,
			CmsAttributes.OWNER_NAME, CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_STRING,
			CmsAttributes.ACL_DOMAIN, CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER);

		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.BINDING_CONDITION, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.BINDING_LABEL, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		// We don't use these, but we should keep them from being copied over
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.LOCAL_FOLDER_LINK, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.REFERENCE_DB_NAME, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.REFERENCE_BY_ID, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.REFERENCE_BY_NAME, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.DOCUMENT, CmsDataType.DF_ID,
			CmsAttributes.REFRESH_INTERVAL, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		CmsDocument.HANDLERS_READY = true;
	}

	private TemporaryPermission antecedentTemporaryPermission = null;
	private TemporaryPermission branchTemporaryPermission = null;
	private List<IDfDocument> priorVersions = null;
	private List<IDfDocument> laterVersions = null;

	public CmsDocument() {
		super(IDfDocument.class);
		CmsDocument.initHandlers();
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
	protected String calculateLabel(IDfDocument document) throws DfException, CMSMFException {
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
		throw new CMSMFException(String.format("None of the parent paths for object [%s] were found", document
			.getObjectId().getId()));
	}

	@Override
	protected String calculateBatchId(IDfDocument document) throws DfException {
		return document.getChronicleId().getId();
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfDocument document, CmsTransferContext ctx)
		throws DfException, CMSMFException {
		super.getDataProperties(properties, document, ctx);

		if (!isDfReference(document)) {
			// Populate the version history, because this will also trigger the population of any
			// required version patches
			getVersionHistory(document, ctx);
			List<IDfValue> patches = getVersionPatches(document, ctx);
			if ((patches != null) && !patches.isEmpty()) {
				properties.add(new CmsProperty(CmsSysObject.VERSION_PATCHES, CmsDataType.DF_STRING, true, patches));
			}
			IDfValue patchAntecedent = getPatchAntecedent(document, ctx);
			if (patchAntecedent != null) {
				properties.add(new CmsProperty(CmsSysObject.PATCH_ANTECEDENT, CmsDataType.DF_STRING, false,
					patchAntecedent));
			}
			return;
		}

		final IDfSession session = document.getSession();

		// TODO: this is untidy - using an undocumented API??
		IDfReference ref = ReferenceFinder.getForMirrorId(document.getObjectId(), session);
		properties.add(new CmsProperty(CmsAttributes.BINDING_CONDITION, CmsDataType.DF_STRING, false, DfValueFactory
			.newStringValue(ref.getBindingCondition())));
		properties.add(new CmsProperty(CmsAttributes.BINDING_LABEL, CmsDataType.DF_STRING, false, DfValueFactory
			.newStringValue(ref.getBindingLabel())));
		properties.add(new CmsProperty(CmsAttributes.LOCAL_FOLDER_LINK, CmsDataType.DF_STRING, false, DfValueFactory
			.newStringValue(ref.getLocalFolderLink())));
		properties.add(new CmsProperty(CmsAttributes.REFERENCE_DB_NAME, CmsDataType.DF_STRING, false, DfValueFactory
			.newStringValue(ref.getReferenceDbName())));
		properties.add(new CmsProperty(CmsAttributes.REFERENCE_BY_ID, CmsDataType.DF_ID, false, DfValueFactory
			.newIdValue(ref.getReferenceById())));
		properties.add(new CmsProperty(CmsAttributes.REFERENCE_BY_NAME, CmsDataType.DF_STRING, false, DfValueFactory
			.newStringValue(ref.getReferenceByName())));
		properties.add(new CmsProperty(CmsAttributes.REFRESH_INTERVAL, CmsDataType.DF_INTEGER, false, DfValueFactory
			.newIntValue(ref.getRefreshInterval())));
	}

	@Override
	protected IDfDocument locateInCms(CmsTransferContext ctx) throws CMSMFException, DfException {
		final IDfSession session = ctx.getSession();

		if (isReference()) { return locateExistingByPath(ctx); }

		// First things first: are we the root of the version hierarchy?
		String sourceChronicleId = getAttribute(CmsAttributes.I_CHRONICLE_ID).getValue().asId().getId();
		final String implicitLabel = getAttribute(CmsAttributes.R_VERSION_LABEL).getValue().asString();

		IDfDocument existing = null;

		final String chronicleId;

		// Map to the new chronicle ID, from the old one...try for the quick win
		final Mapping chronicleMapping = ctx.getAttributeMapper().getTargetMapping(getType(),
			CmsAttributes.R_OBJECT_ID, sourceChronicleId);

		// If we don't have a chronicle mapping, we're likely the root document and thus
		// will have to search by path...
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
			if (existing == null) { return null; }

			// We have a match, but it may not be the version we seek, so
			// track the chronicle so the code below can find the right version.
			chronicleId = existing.getChronicleId().getId();
		}

		// Using the chronicle ID and the implicit version ID, we will seek out
		// the exact existing version.
		IDfPersistentObject obj = session.getObjectByQualification(String
			.format("dm_sysobject (all) where i_chronicle_id = '%s' and any r_version_label = '%s'", chronicleId,
				implicitLabel));

		// Return whatever we found...if we found nothing, then this is a new version
		// and must be handled as such
		return castObject(obj);
	}

	private List<IDfDocument> getVersions(boolean prior, IDfDocument document, CmsTransferContext ctx)
		throws DfException, CMSMFException {
		if (document == null) { throw new IllegalArgumentException("Must provide a document whose versions to analyze"); }

		if ((this.priorVersions == null) || (this.laterVersions == null)) {
			final List<IDfDocument> priorVersions = new LinkedList<IDfDocument>();
			final List<IDfDocument> laterVersions = new LinkedList<IDfDocument>();
			List<IDfDocument> target = priorVersions;
			List<IDfDocument> history = getVersionHistory(document, ctx);
			final String limitId = document.getObjectId().getId();
			for (IDfDocument d : history) {
				if (Tools.equals(d.getObjectId().getId(), limitId)) {
					// Once we've found the "reference" object in the history, we skip adding it
					// since it will be added explicitly, and we start adding later versions
					target = laterVersions;
					continue;
				}
				target.add(d);
			}
			this.priorVersions = priorVersions;
			this.laterVersions = laterVersions;
		}
		return (prior ? this.priorVersions : this.laterVersions);
	}

	@Override
	protected void doPersistRequirements(IDfDocument document, CmsTransferContext ctx,
		CmsDependencyManager dependencyManager) throws DfException, CMSMFException {

		final IDfSession session = document.getSession();

		// The parent folders
		final int pathCount = document.getFolderIdCount();
		for (int i = 0; i < pathCount; i++) {
			IDfId folderId = document.getFolderId(i);
			IDfFolder parent = session.getFolderBySpecification(folderId.getId());
			dependencyManager.persistRelatedObject(parent);
		}

		// We do nothing else for references, as we need nothing else
		if (isDfReference(document)) { return; }

		// Export the object type
		dependencyManager.persistRelatedObject(document.getType());

		// Export the format
		IDfFormat format = document.getFormat();
		if (format != null) {
			dependencyManager.persistRelatedObject(format);
		}

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		if (Tools.equals(getId(), ctx.getRootObjectId())) {
			// Now, also do the *PREVIOUS* versions... we'll do the later versions as dependents
			for (IDfDocument versionDoc : getVersions(true, document, ctx)) {
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Adding prior version [%s]", calculateVersionString(document, false)));
				}
				dependencyManager.persistRelatedObject(versionDoc);
			}
		}

		// We export our contents...
		CmsProperty contents = new CmsProperty(CmsDocument.CONTENTS, CmsDataType.DF_ID, true);
		setProperty(contents);
		String dql = "" //
			+ "select dcs.r_object_id " //
			+ "  from dmr_content_r dcr, dmr_content_s dcs " //
			+ " where dcr.r_object_id = dcs.r_object_id " //
			+ "   and dcr.parent_id = '%s' " //
			+ "   and dcr.page = %d " //
			+ " order by dcs.rendition ";
		final String parentId = getId();
		final int pageCount = document.getPageCount();
		for (int i = 0; i < pageCount; i++) {
			IDfCollection results = DfUtils.executeQuery(session, String.format(dql, parentId, i),
				IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					contents.addValue(results.getValue("r_object_id"));
				}
			} finally {
				DfUtils.closeQuietly(results);
			}
		}
	}

	@Override
	protected void doPersistDependents(IDfDocument document, CmsTransferContext ctx,
		CmsDependencyManager dependencyManager) throws DfException, CMSMFException {

		final IDfSession session = document.getSession();

		dependencyManager.persistRelatedObject(document.getACL());

		// References need only the ACL as a dependent
		if (isDfReference(document)) { return; }

		String owner = CmsMappingUtils.substituteMappableUsers(session, document.getOwnerName());
		if (!CmsMappingUtils.isSubstitutionForMappableUser(owner)) {
			IDfUser user = session.getUser(document.getOwnerName());
			if (user != null) {
				dependencyManager.persistRelatedObject(user);
			}
		}

		IDfGroup group = session.getGroup(document.getGroupName());
		if (group != null) {
			dependencyManager.persistRelatedObject(group);
		}

		// Save filestore name
		// String storageType = document.getStorageType();
		// if (StringUtils.isNotBlank(storageType)) {
		// RepositoryConfiguration.getRepositoryConfiguration().addFileStore(storageType);
		// }

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		if (Tools.equals(getId(), ctx.getRootObjectId())) {
			// Now, also do the *SUBSEQUENT* versions...
			for (IDfDocument versionDoc : getVersions(false, document, ctx)) {
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Adding subsequent version [%s]",
						calculateVersionString(document, false)));
				}
				dependencyManager.persistRelatedObject(versionDoc);
			}
		}

		// Now, export the content
		ctx.setValue(CmsContent.DOCUMENT_ID, document.getValue(CmsAttributes.R_OBJECT_ID));
		for (IDfValue contentId : getProperty(CmsDocument.CONTENTS)) {
			IDfPersistentObject content = session.getObject(contentId.asId());
			dependencyManager.persistRelatedObject(content);
		}
	}

	@Override
	protected boolean isVersionable(IDfDocument object) throws DfException {
		// TODO: Are references versionable, per-se?
		return true;
	}

	@Override
	protected IDfId persistChanges(IDfDocument document, CmsTransferContext context) throws DfException, CMSMFException {
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

	protected IDfDocument newReference(CmsTransferContext context) throws DfException, CMSMFException {
		IDfPersistentObject target = null;
		IDfSession session = context.getSession();
		IDfValue bindingCondition = getProperty(CmsAttributes.BINDING_CONDITION).getValue();
		IDfValue bindingLabel = getProperty(CmsAttributes.BINDING_LABEL).getValue();
		IDfValue referenceById = getProperty(CmsAttributes.REFERENCE_BY_ID).getValue();

		target = session.getObject(referenceById.asId());
		if (!(target instanceof IDfSysObject)) { throw new CMSMFException(String.format(
			"Reference [%s] target object [%s] is not an IDfSysObject instance", getLabel(), referenceById.asString())); }

		IDfSysObject targetSysObj = IDfSysObject.class.cast(target);
		IDfValue mainFolderAtt = getProperty(CmsSysObject.TARGET_PARENTS).getValue();
		Mapping m = context.getAttributeMapper().getTargetMapping(CmsObjectType.FOLDER, CmsAttributes.R_OBJECT_ID,
			mainFolderAtt.asString());
		if (m == null) { throw new CMSMFException(String.format(
			"Reference [%s] mapping for its parent folder [%s->???] could not be found", getLabel(),
			mainFolderAtt.asString())); }

		IDfId mainFolderId = new DfId(m.getTargetValue());
		// TODO: Can a reference be *linked* to other folders?
		IDfId newId = targetSysObj.addReference(mainFolderId, bindingCondition.asString(), bindingLabel.asString());
		return castObject(session.getObject(newId));
	}

	@Override
	protected IDfDocument newObject(CmsTransferContext context) throws DfException, CMSMFException {

		if (isReference()) { return newReference(context); }

		final String sourceChronicleId = getAttribute(CmsAttributes.I_CHRONICLE_ID).getValue().asId().getId();
		final boolean root = (Tools.equals(getId(), sourceChronicleId));
		if (root) { return super.newObject(context); }

		final IDfSession session = context.getSession();

		final IDfId antecedentId;
		IDfDocument antecedentVersion = null;
		final CmsProperty antecedentProperty = getProperty(CmsSysObject.PATCH_ANTECEDENT);
		if (antecedentProperty == null) {
			antecedentId = getAttribute(CmsAttributes.I_ANTECEDENT_ID).getValue().asId();
		} else {
			IDfId aid = antecedentProperty.getValue().asId();
			if (aid.isObjectId()) {
				antecedentId = aid;
			} else {
				antecedentId = null;
				Mapping mapping = context.getAttributeMapper().getTargetMapping(getType(), CmsAttributes.R_OBJECT_ID,
					sourceChronicleId);
				if (mapping == null) {
					// The root of the trunk is missing...we'll need to create a new, contentless
					// object
					antecedentVersion = super.newObject(context);

					// Set the name
					antecedentVersion.setObjectName(getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString());

					// Set the owner and group
					antecedentVersion.setOwnerName(getAttribute(CmsAttributes.OWNER_NAME).getValue().asString());
					antecedentVersion.setGroupName(getAttribute(CmsAttributes.GROUP_NAME).getValue().asString());

					// Set the ACL
					antecedentVersion.setACLDomain(getAttribute(CmsAttributes.ACL_DOMAIN).getValue().asString());
					antecedentVersion.setACLName(getAttribute(CmsAttributes.ACL_NAME).getValue().asString());

					// Link to prospective parents
					// TODO: Mess with parents' permissions?
					linkToParents(antecedentVersion, context);

					// Create the chronicle mapping
					// TODO: How do we revert this if the transaction fails later on?
					context.getAttributeMapper().setMapping(getType(), CmsAttributes.R_OBJECT_ID, sourceChronicleId,
						antecedentVersion.getChronicleId().getId());

					// And...finally...
					// TODO: Need a "simple" way to modify the r_modify_date for the document
					updateSystemAttributes(antecedentVersion, context);
				} else {
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
			Mapping mapping = context.getAttributeMapper().getTargetMapping(getType(), CmsAttributes.R_OBJECT_ID,
				antecedentId.getId());
			if (mapping == null) { throw new CMSMFException(String.format(
				"Can't create a new version of [%s](%s) - antecedent version not found [%s]", getLabel(), getId(),
				antecedentId)); }
			IDfId id = new DfId(mapping.getTargetValue());
			session.flushObject(id);
			antecedentVersion = castObject(session.getObject(id));
		}

		CmsProperty patches = getProperty(CmsSysObject.VERSION_PATCHES);
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
				final CmsProperty prop = new CmsProperty(CmsAttributes.R_VERSION_LABEL, CmsDataType.DF_STRING, false,
					DfValueFactory.newStringValue(p.toString()));
				IDfDocument patchDocument = createSuccessorVersion(antecedentVersion, prop, context);
				IDfId checkinId = persistNewVersion(patchDocument, p.asString(), context);
				cleanUpTemporaryPermissions(session);
				resetMutabilityFlags();

				// If we branched, we don't change antecedents
				lastAntecedent = castObject(session.getObject(checkinId));
				lastAntecedentVersion = p;
				if (!context.getValue(CmsSysObject.BRANCH_MARKER).asBoolean()) {
					antecedentVersion = lastAntecedent;
				}
			}

			// If this version is to be a successor of the last antecedent (all components are the
			// same except the last number), then we go ahead and assign it to antecedentVersion. If
			// it's a sibling (same-level branch), or a descendant (sub-branch), then we leave the
			// antecedent where it is
			DfVersionNumber newVersion = new DfVersionNumber(getAttribute(CmsAttributes.R_VERSION_LABEL).getValue()
				.asString());
			DfVersionNumber lastVersion = new DfVersionNumber(lastAntecedentVersion.asString());
			if (newVersion.isSuccessorOf(lastVersion)) {
				antecedentVersion = lastAntecedent;
			}
		}

		return createSuccessorVersion(antecedentVersion, null, context);
	}

	private IDfDocument createSuccessorVersion(IDfDocument antecedentVersion, CmsProperty rVersionLabel,
		CmsTransferContext context) throws DfException {
		final IDfSession session = antecedentVersion.getSession();
		antecedentVersion.fetch(null);
		this.antecedentTemporaryPermission = new TemporaryPermission(antecedentVersion, IDfACL.DF_PERMIT_DELETE);
		if (this.antecedentTemporaryPermission.grant(antecedentVersion)) {
			antecedentVersion.save();
		}

		// This is so later on we can decide whether we should set or clear the immutable flag,
		// but only if we're a leaf node
		CmsAttribute descendantCount = getAttribute(CmsAttributes.I_DIRECT_DSC);
		if ((descendantCount != null) && (descendantCount.getValue().asInteger() == 0)) {
			detectIncomingMutability();
		}

		if (rVersionLabel == null) {
			rVersionLabel = getAttribute(CmsAttributes.R_VERSION_LABEL);
		}
		String antecedentVersionImplicitVersionLabel = antecedentVersion.getImplicitVersionLabel();
		String documentImplicitVersionLabel = rVersionLabel.getValue().asString();

		int antecedentDots = StringUtils.countMatches(antecedentVersionImplicitVersionLabel, ".");
		int documentDots = StringUtils.countMatches(documentImplicitVersionLabel, ".");

		final boolean shouldBranch = (documentDots == (antecedentDots + 2));
		context.setValue(CmsSysObject.BRANCH_MARKER, DfValueFactory.newBooleanValue(shouldBranch));
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

	@Override
	protected void prepareOperation(IDfDocument sysObject, boolean newObject) throws DfException, CMSMFException {
		super.prepareOperation(sysObject, newObject);
		if (!newObject) {
			// We only do this for old objects because new objects will have been
			// "fixed" in this respect in newObject()
			detectAndClearMutability(sysObject);
		}
	}

	@Override
	protected void prepareForConstruction(IDfDocument document, boolean newObject, CmsTransferContext context)
		throws DfException {

		// Is root?
		String sourceChronicleId = getAttribute(CmsAttributes.I_CHRONICLE_ID).getValue().asId().getId();
		final boolean root = (Tools.equals(getId(), sourceChronicleId));
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
	protected void finalizeConstruction(final IDfDocument document, boolean newObject, final CmsTransferContext context)
		throws DfException, CMSMFException {

		// References don't require any of this being done
		if (isReference()) { return; }

		final IDfSession session = document.getSession();

		// Now, create the content the contents
		Set<String> contentIds = new HashSet<String>();
		for (IDfValue contentId : getProperty(CmsDocument.CONTENTS)) {
			contentIds.add(contentId.asString());
		}

		final String documentId = document.getObjectId().getId();
		final String contentType = getAttribute(CmsAttributes.A_CONTENT_TYPE).getValue().toString();
		final CmsFileSystem fs = context.getFileSystem();
		final int contentCount = contentIds.size();
		context.deserializeObjects(CmsContent.class, contentIds, new ObjectHandler() {

			private final AtomicInteger current = new AtomicInteger(0);

			@Override
			public boolean newBatch(String batchId) throws CMSMFException {
				this.current.set(0);
				String msg = String.format("Content addition started for document [%s](%s)", getLabel(), getId());
				CmsDocument.this.log.info(msg);
				context.printf("\t%s (%d items)", msg, contentCount);
				return true;
			}

			@Override
			public void handle(CmsObject<?> obj) throws CMSMFException {
				// Step one: what's the content's path in the filesystem?
				if (!(obj instanceof CmsContent)) { return; }
				final CmsContent content = CmsContent.class.cast(obj);
				final File path = content.getFsPath();
				final File fullPath;
				try {
					fullPath = fs.getContentFile(path);
				} catch (IOException e) {
					throw new CMSMFException(String.format("Failed to locate the actual content path for [%s]",
						path.getPath()), e);
				}
				final String absolutePath = fullPath.getAbsolutePath();

				final int pageNumber = content.getAttribute(CmsAttributes.PAGE).getValue().asInteger();
				final CmsAttribute renditionNumber = content.getAttribute(CmsAttributes.RENDITION);
				final CmsAttribute pageModifierAtt = content.getAttribute(CmsAttributes.PAGE_MODIFIER);
				final String pageModifier = (pageModifierAtt.hasValues() ? pageModifierAtt.getValue().asString()
					: CmsDataType.DF_STRING.getNullEncoding());
				String fullFormat = content.getAttribute(CmsAttributes.FULL_FORMAT).getValue().asString();

				if ((renditionNumber == null) || (renditionNumber.getValue().asInteger() == 0)) {
					if ((fullFormat != null) && !Tools.equals(fullFormat, contentType)) {
						fullFormat = contentType;
					}
					try {
						document.setFileEx(absolutePath, fullFormat, pageNumber, null);
						final String msg = String.format(
							"Added the primary content to document [%s](%s) -> {%s/%s/%s}", getLabel(), getId(),
							absolutePath, fullFormat, pageNumber);
						CmsDocument.this.log.info(msg);
						context.printf("\t%s (item %d of %d)", msg, this.current.incrementAndGet(), contentCount);
					} catch (DfException e) {
						final String msg = String.format(
							"Failed to add the primary content to document [%s](%s) -> {%s/%s/%s}", getLabel(),
							getId(), absolutePath, fullFormat, pageNumber);
						context.printf("\t%s (item %d of %d): %s [%s]", msg, this.current.incrementAndGet(),
							contentCount, e.getClass().getCanonicalName(), e.getMessage());
						throw new CMSMFException(msg, e);
					}
				} else {
					try {
						document.addRenditionEx2(absolutePath, fullFormat, pageNumber, pageModifier, null, false,
							false, false);
						final String msg = String.format(
							"Added rendition content to document [%s](%s) -> {%s/%s/%s/%s}", getLabel(), getId(),
							absolutePath, fullFormat, pageNumber, pageModifier);
						CmsDocument.this.log.info(msg);
						context.printf("\t%s (item %d of %d)", msg, this.current.incrementAndGet(), contentCount);
					} catch (DfException e) {
						final String msg = String.format(
							"Failed to add rendition content to document [%s](%s) -> {%s/%s/%s/%s}", getLabel(),
							getId(), absolutePath, fullFormat, pageNumber, pageModifier);
						context.printf("\t%s (item %d of %d): %s [%s]", msg, this.current.incrementAndGet(),
							contentCount, e.getClass().getCanonicalName(), e.getMessage());
						throw new CMSMFException(msg, e);
					}
				}

				String setFile = content.getAttribute(CmsAttributes.SET_FILE).getValue().asString();
				if (StringUtils.isBlank(setFile)) {
					setFile = " ";
				}
				// If setFile contains single quote in its contents, to escape it, replace it
				// with 4 single quotes.
				setFile = setFile.replaceAll("'", "''''");

				String setClient = content.getAttribute(CmsAttributes.SET_CLIENT).getValue().asString();
				if (StringUtils.isBlank(setClient)) {
					setClient = " ";
				}

				IDfTime setTime = content.getAttribute(CmsAttributes.SET_TIME).getValue().asTime();

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
					sql = String.format(sql, setFile, setClient, DfUtils.generateSqlDateClause(setTime, session),
						documentId, renditionNumber.getValue().asInteger(), pageModifierClause, pageNumber, fullFormat);
					if (!runExecSQL(session, sql)) {
						final String msg = String
							.format(
								"SQL Execution failed for updating the content's system attributes for document [%s](%s) -> {%s/%s/%s/%s}:%n%s%n",
								getLabel(), getId(), absolutePath, fullFormat, pageNumber, pageModifier, sql);
						context.printf("\t%s (item %d of %d)", msg, this.current.incrementAndGet(), contentCount);
						throw new CMSMFException(msg);
					}
				} catch (DfException e) {
					final String msg = String
						.format(
							"Exception caught generating updating the content's system attributes for document [%s](%s) -> {%s/%s/%s/%s}",
							getLabel(), getId(), absolutePath, fullFormat, pageNumber, pageModifier);
					context.printf("\t%s (item %d of %d): %s [%s]", msg, this.current.incrementAndGet(), contentCount,
						e.getClass().getCanonicalName(), e.getMessage());
					throw new CMSMFException(msg, e);
				}
			}

			@Override
			public boolean closeBatch(boolean ok) throws CMSMFException {
				String msg = String.format("Content addition finished for document [%s](%s)", getLabel(), getId());
				CmsDocument.this.log.info(msg);
				context.printf("\t%s (%d items)", msg, contentCount);
				return true;
			}

		});

		// Now, link to the parent folders
		linkToParents(document, context);
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
	protected boolean cleanupAfterSave(IDfDocument document, boolean newObject, CmsTransferContext context)
		throws DfException, CMSMFException {
		final IDfSession session = document.getSession();

		cleanUpParents(session);
		cleanUpTemporaryPermissions(session);

		return super.cleanupAfterSave(document, newObject, context);
	}
}