/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cms.CmsAttributeMapper.Mapping;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsDocument extends CmsObject<IDfDocument> {

	private static final String TARGET_PATHS = "targetPaths";
	private static final String TARGET_PARENTS = "targetParents";
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

		CmsDocument.HANDLERS_READY = true;
	}

	private PermitDelta antecedentPermitDelta = null;
	private PermitDelta branchPermitDelta = null;
	private Map<String, PermitDelta> parentFolderDeltas = null;

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
	protected String calculateLabel(IDfDocument document) throws DfException {
		IDfId id = document.getFolderId(0);
		String path = "(unknown)";
		if (id != null) {
			IDfFolder f = IDfFolder.class.cast(document.getSession().getObject(id));
			if (f != null) {
				path = f.getFolderPath(0);
			}
		}
		return String.format("%s/%s [%s]", path, document.getObjectName(), calculateVersionString(document, true));
	}

	@Override
	protected String calculateBatchId(IDfDocument document) throws DfException {
		return document.getChronicleId().getId();
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfDocument document) throws DfException {
		CmsProperty paths = new CmsProperty(CmsDocument.TARGET_PATHS, CmsDataType.DF_STRING, true);
		properties.add(paths);
		CmsProperty parents = new CmsProperty(CmsDocument.TARGET_PARENTS, CmsDataType.DF_ID, true);
		properties.add(parents);
		final IDfSession session = document.getSession();
		for (IDfValue folderId : getAttribute(CmsAttributes.I_FOLDER_ID)) {
			IDfFolder parent = session.getFolderBySpecification(folderId.asId().getId());
			if (parent == null) { throw new DfException(String.format(
				"Document [%s](%s) references non-existent folder [%s]", getLabel(), getId(), folderId.asString())); }
			parents.addValue(folderId);
			int pathCount = parent.getFolderPathCount();
			for (int i = 0; i < pathCount; i++) {
				paths.addValue(DfValueFactory.newStringValue(parent.getFolderPath(i)));
			}
		}
	}

	@Override
	protected IDfDocument locateInCms(CmsTransferContext ctx) throws CMSMFException, DfException {
		final IDfSession session = ctx.getSession();

		// First things first: are we the root of the version hierarchy?
		String sourceChronicleId = getAttribute(CmsAttributes.I_CHRONICLE_ID).getValue().asId().getId();
		final boolean root = (Tools.equals(getId(), sourceChronicleId));
		final String implicitLabel = getAttribute(CmsAttributes.R_VERSION_LABEL).getValue().asString();

		IDfDocument existing = null;

		if (!root) {
			// Ok so this isn't the root version... can we find a version attached
			// to the new chronicle ID (which will already have been mapped) that
			// matches this document's same implicit version label?

			// Map to the new chronicle ID, from the old one...
			Mapping chronicleMapping = ctx.getAttributeMapper().getTargetMapping(getType(), CmsAttributes.R_OBJECT_ID,
				sourceChronicleId);
			if (chronicleMapping != null) {
				// We have the chronicle! Try to find our actual match!
				IDfPersistentObject obj = session.getObjectByQualification(String.format(
					"%s where i_chronicle_id = '%s' and any r_version_label = '%s'",
					String.format("%s (ALL)", getSubtype()), chronicleMapping.getTargetValue(), implicitLabel));
				// If we have it, return it!!
				if (obj != null) { return castObject(obj); }
			}

			// Ok...so we didn't find our version on that version tree...
			if (implicitLabel != null) { return null; }

			// These are adapted versions of Shridev's lookup code...
			existing = locateIdenticalDocument(session);
			if (existing != null) { return existing; }

			// Nothing there? Ok... let's try to map the chronicle...
			final CmsAttribute antecedent = getAttribute(CmsAttributes.I_ANTECEDENT_ID);
			final IDfId antecedentId = (antecedent != null ? antecedent.getValue().asId() : DfId.DF_NULLID);
			if (!antecedentId.isNull()) {
				Mapping mapping = ctx.getAttributeMapper().getTargetMapping(getType(), CmsAttributes.R_OBJECT_ID,
					antecedentId.getId());
				if (mapping != null) {
					IDfPersistentObject antecedentObject = session.getObject(new DfId(mapping.getTargetValue()));
					if (antecedentObject != null) {
						existing = locateSimilarDescendant(antecedentObject);
					}
				}
			}
		}

		// We're the root, we can only search by path...we look at all the paths this
		// object is expected to take up on the target, and they must refer to either
		// no existing object, or exactly one existing object. If there is an existing
		// object, it's replaced by this one.
		final String documentName = getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString();
		String existingPath = null;
		// We could do this "the hard way" by seeking out each parent by ID from TARGET_PARENTS,
		// but we pre-calculated the target paths when we exported the object, so there...sue me :)
		for (IDfValue p : getProperty(CmsDocument.TARGET_PATHS)) {
			String currentPath = String.format("%s/%s", p.asString(), documentName);
			IDfPersistentObject current = session.getObjectByPath(currentPath);
			if (current == null) {
				// No match, we're good...
				continue;
			}
			if (!(current instanceof IDfDocument)) {
				// Not a document, so we're not interested
				continue;
			}
			IDfDocument currentDoc = IDfDocument.class.cast(current);
			if (existing == null) {
				// First match, keep track of it
				existing = currentDoc;
				existingPath = currentPath;
				continue;
			}
			// Second match, is it the same as the first?
			if (Tools.equals(existing.getObjectId().getId(), current.getObjectId().getId())) {
				// Same as the first - we have an issue here
				continue;
			}
			// Not the same, this is a problem
			throw new CMSMFException(String.format(
				"Found two different documents matching this document's paths: [%s@%s] and [%s@%s]", existing
					.getObjectId().getId(), existingPath, current.getObjectId().getId(), currentPath));
		}
		return existing;
	}

	private IDfDocument locateIdenticalDocument(IDfSession session) throws DfException {
		String objectName = getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString();
		// If object name contains single quote, replace it with 2 single quotes for DQL
		objectName = objectName.replaceAll("'", "''");

		final String objectType = getSubtype();
		final IDfTime createDate = getAttribute(CmsAttributes.R_CREATION_DATE).getValue().asTime();

		// Get the first folder location
		final CmsProperty targetPaths = getProperty(CmsDocument.TARGET_PATHS);
		String folderLocation = (targetPaths.hasValues() ? targetPaths.getValue(0).asString() : "");

		// If folder location contains single quote, replace it with 2 single quotes for DQL
		folderLocation = folderLocation.replaceAll("'", "''");

		// NOTE: Check the i_is_deleted flag. If the object is marked as deleted, the dql will never
		// find the matching document. Marked as deleted objects need to be queried by going
		// directly against the dm_sysobject_s table. When you use dm_sysobject_s table in from
		// clause, you can't use folder predicate. Find out the r_object_id of the /Temp cabinet and
		// use it in the query.

		final String objectSet;
		final String extraCond;
		final String extraTerm;
		if (getAttribute(CmsAttributes.I_IS_DELETED).getValue().asBoolean()) {
			objectSet = "dm_sysobject_s";
			extraCond = "i_cabinet_id = '%s'";

			IDfFolder tempCabinet = session.getFolderByPath("/Temp");
			extraTerm = tempCabinet.getObjectId().getId();
		} else {
			objectSet = String.format("%s (ALL)", objectType);
			extraCond = "folder('%s')";
			extraTerm = folderLocation;
		}

		final String qualification = String.format(
			"%s where object_name = '%s' and %s and r_creation_date = DATE('%s')", objectSet, objectName,
			String.format(extraCond, extraTerm), createDate.asString(Constant.DCTM_DATETIME_PATTERN));
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Finding identical object using DQL: %s", qualification));
		}

		// Retrieve the object using the query
		return castObject(session.getObjectByQualification(qualification));
	}

	private IDfDocument locateSimilarDescendant(IDfPersistentObject antecedentVersion) throws DfException {
		final IDfSession session = antecedentVersion.getSession();

		String antecedentId = antecedentVersion.getObjectId().getId();
		String versionLabel = getAttribute(CmsAttributes.R_VERSION_LABEL).getValue().asString();
		String objectType = getSubtype();

		// Retrieve the object using the query
		return castObject(session.getObjectByQualification(String.format(
			"%s (ALL) where i_antecedent_id = '%s' and any r_version_label = '%s'", objectType, antecedentId,
			versionLabel)));
	}

	private List<IDfId> getVersions(boolean prior, IDfDocument document) throws DfException {
		if (document == null) { throw new IllegalArgumentException("Must provide a document whose versions to analyze"); }

		// Is this the root of the version hierarchy? If so, then there are no prior versions
		if (prior && Tools.equals(document.getObjectId().getId(), document.getChronicleId().getId())) {
			// Return an empty list - this is the root of the version hierarchy
			return new ArrayList<IDfId>();
		}

		IDfCollection versions = document.getVersions(null);
		try {
			final IDfId currentId = document.getObjectId();
			LinkedList<IDfId> ret = new LinkedList<IDfId>();
			boolean caughtUp = false;
			while (versions.next()) {
				IDfId versionId = versions.getId("r_object_id");
				if (versionId.isNull()) {
					// Shouldn't happen, but better safe than sorry...
					continue;
				}

				boolean current = Tools.equals(currentId.getId(), versionId.getId());
				caughtUp |= current;
				// This logic can be condensed, but it's better to leave it simple
				// to understand
				if (prior) {
					// If we're looking for prior versions, then we have to wait until
					// we find this one, and then start adding
					if (!caughtUp || current) {
						continue;
					}
				} else {
					// If we're looking for later versions, then we start adding them
					// all, until we find this one
					if (caughtUp) {
						// We've caught up with the present, break the cycle
						break;
					}
				}

				// Add this version at the head, since it's older than the existing ones
				ret.addFirst(versionId);
			}

			return ret;
		} finally {
			DfUtils.closeQuietly(versions);
		}
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
		if (document.isReference()) { return; }

		// Export the object type
		dependencyManager.persistRelatedObject(document.getType());

		// Export the format
		dependencyManager.persistRelatedObject(document.getFormat());

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		if (Tools.equals(getId(), ctx.getRootObjectId())) {
			// TODO: This works for single-threadedness... for multi-threadedness, they must be
			// stored as separate types
			// Now, also do the *PREVIOUS* versions... we'll do the later versions as dependents
			for (IDfId versionId : getVersions(true, document)) {
				IDfPersistentObject obj = session.getObject(versionId);
				if (obj == null) {
					// WTF?? Shouldn't happen...
					continue;
				}
				IDfDocument versionDoc = IDfDocument.class.cast(obj);
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

		// We do nothing else for references, as we need nothing else
		if (document.isReference()) { return; }

		final IDfSession session = document.getSession();

		String owner = CmsMappingUtils.resolveSpecialUser(session, document.getOwnerName());
		if (!CmsMappingUtils.isSpecialUserSubstitution(owner)) {
			IDfUser user = session.getUser(document.getOwnerName());
			if (user != null) {
				dependencyManager.persistRelatedObject(user);
			}
		}

		// Do the others
		IDfPersistentObject[] dep = {
			// The group
			session.getGroup(document.getGroupName()),
			// The ACL
			document.getACL()
		};
		for (IDfPersistentObject obj : dep) {
			if (obj == null) {
				continue;
			}
			dependencyManager.persistRelatedObject(obj);
		}

		// Save filestore name
		String storageType = document.getStorageType();
		if (StringUtils.isNotBlank(storageType)) {
			RepositoryConfiguration.getRepositoryConfiguration().addFileStore(storageType);
		}

		// We only export versions if we're the root object of the context operation
		// There is no actual harm done, since the export engine is smart enough to
		// not duplicate, but doing it like this helps us avoid o(n^2) performance
		// which is BAAAD
		if (Tools.equals(getId(), ctx.getRootObjectId())) {
			// Now, also do the *SUBSEQUENT* versions...
			for (IDfId versionId : getVersions(false, document)) {
				IDfPersistentObject obj = session.getObject(versionId);
				if (obj == null) {
					// WTF?? Shouldn't happen...
					continue;
				}
				IDfDocument versionDoc = IDfDocument.class.cast(obj);
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Adding subsequent version [%s]",
						calculateVersionString(document, false)));
				}
				dependencyManager.persistRelatedObject(versionDoc);
			}
		}

		// Now, export the content
		for (IDfValue contentId : getProperty(CmsDocument.CONTENTS)) {
			IDfPersistentObject content = session.getObject(contentId.asId());
			if (content == null) {
				// Impossible, but defend against it anyway
				this.log.warn(String.format("Missing content %s for document [%s](%s)", contentId.asString(),
					getLabel(), getId()));
				continue;
			}
			dependencyManager.persistRelatedObject(content);
		}
	}

	@Override
	protected boolean isVersionable(IDfDocument object) throws DfException {
		return true;
	}

	@Override
	protected IDfDocument newObject(CmsTransferContext context) throws DfException, CMSMFException {
		String sourceChronicleId = getAttribute(CmsAttributes.I_CHRONICLE_ID).getValue().asId().getId();
		final boolean root = (Tools.equals(getId(), sourceChronicleId));
		if (root) { return super.newObject(context); }

		IDfSession session = context.getSession();

		String antecedentId = getAttribute(CmsAttributes.I_ANTECEDENT_ID).getValue().asString();
		Mapping mapping = context.getAttributeMapper().getTargetMapping(getType(), CmsAttributes.R_OBJECT_ID,
			antecedentId);
		if (mapping == null) { throw new CMSMFException(String.format(
			"Can't create version - antecedent version not found [%s]", antecedentId)); }
		IDfDocument antecedentVersion = castObject(session.getObject(new DfId(mapping.getTargetValue())));
		antecedentVersion.fetch(null);
		this.antecedentPermitDelta = new PermitDelta(antecedentVersion, IDfACL.DF_PERMIT_VERSION);
		if (this.antecedentPermitDelta.grant(antecedentVersion)) {
			antecedentVersion.save();
		}

		CmsAttribute rVersionLabel = getAttribute(CmsAttributes.R_VERSION_LABEL);
		String antecedentVersionImplicitVersionLabel = antecedentVersion.getImplicitVersionLabel();
		String documentImplicitVersionLabel = rVersionLabel.getValue().asString();

		int antecedentDots = StringUtils.countMatches(antecedentVersionImplicitVersionLabel, ".");
		int documentDots = StringUtils.countMatches(documentImplicitVersionLabel, ".");

		if (documentDots == (antecedentDots + 2)) {
			// branch
			IDfId branchID = antecedentVersion.branch(antecedentVersionImplicitVersionLabel);
			antecedentVersion = castObject(session.getObject(branchID));

			// remove branch version label from repeating attributes
			// This should be the implicit version label
			rVersionLabel.removeValue(0);
			this.branchPermitDelta = new PermitDelta(antecedentVersion, IDfACL.DF_PERMIT_WRITE);
			this.branchPermitDelta.grant(antecedentVersion);
		} else {
			// checkout
			this.branchPermitDelta = null;
			antecedentVersion.checkout();
		}
		return antecedentVersion;
	}

	@Override
	protected void prepareForConstruction(IDfDocument document, boolean newObject, CmsTransferContext context)
		throws DfException {
		this.parentFolderDeltas = null;

		IDfSession session = document.getSession();
		session.flushCache(false);

		// Is root?
		String sourceChronicleId = getAttribute(CmsAttributes.I_CHRONICLE_ID).getValue().asId().getId();
		final boolean root = (Tools.equals(getId(), sourceChronicleId));
		if (!root && !newObject) {
			this.antecedentPermitDelta = new PermitDelta(document, IDfACL.DF_PERMIT_VERSION);
			if (this.antecedentPermitDelta.grant(document)) {
				// Not sure this is OK...
				if (!document.isCheckedOut()) {
					document.save();
				}
			}
		}

		this.parentFolderDeltas = new HashMap<String, PermitDelta>();
		for (IDfValue parentId : getProperty(CmsDocument.TARGET_PARENTS)) {
			Mapping m = context.getAttributeMapper().getTargetMapping(CmsObjectType.FOLDER, CmsAttributes.R_OBJECT_ID,
				parentId.asString());
			if (m == null) {
				// TODO: HOW??!
				continue;
			}
			String actualParentId = m.getTargetValue();
			IDfFolder parentFolder = session.getFolderBySpecification(actualParentId);

			// Not sure why?!! We just follow the leader here...
			session.flush("persistentobjcache", null);
			session.flushObject(parentFolder.getObjectId());
			session.flushCache(false);

			parentFolder = session.getFolderBySpecification(actualParentId);
			parentFolder.fetch(null);
			// Stow its old permissions
			PermitDelta newDelta = new PermitDelta(parentFolder, IDfACL.DF_PERMIT_WRITE);
			if (newDelta.grant(parentFolder)) {
				parentFolder.save();
			}
			this.parentFolderDeltas.put(actualParentId, newDelta);
		}
	}

	@Override
	protected void finalizeConstruction(IDfDocument document, boolean newObject, CmsTransferContext context)
		throws DfException {
	}

	@Override
	protected boolean postConstruction(IDfDocument object, boolean newObject, CmsTransferContext context)
		throws DfException {
		return super.postConstruction(object, newObject, context);
	}

	@Override
	protected boolean cleanupAfterSave(IDfDocument document, boolean newObject, CmsTransferContext context)
		throws DfException {
		final IDfSession session = document.getSession();
		for (PermitDelta delta : this.parentFolderDeltas.values()) {
			final IDfId parentId = new DfId(delta.getObjectId());
			IDfFolder parentFolder = session.getFolderBySpecification(parentId.getId());
			if (parentFolder == null) {
				// TODO: uhm...how?
				continue;
			}

			// Not sure why?!! We just follow the leader here...
			session.flush("persistentobjcache", null);
			session.flushObject(parentId);
			session.flushCache(false);
			parentFolder = session.getFolderBySpecification(parentId.getId());
			parentFolder.fetch(null);
			// Stow its old permissions
			if (delta.revoke(parentFolder)) {
				parentFolder.save();
			}
		}

		if (this.antecedentPermitDelta != null) {
			IDfId antecedentId = new DfId(this.antecedentPermitDelta.getObjectId());
			IDfDocument antecedent = castObject(session.getObject(antecedentId));
			if (antecedent != null) {
				// When would this be null?
				session.flush("persistentobjcache", null);
				session.flushObject(antecedentId);
				session.flushCache(false);
				antecedent.fetch(null);
				if (this.antecedentPermitDelta.revoke(antecedent)) {
					antecedent.save();
				}
			}
		}

		if (this.branchPermitDelta != null) {
			IDfId branchId = new DfId(this.branchPermitDelta.getObjectId());
			IDfDocument branch = castObject(session.getObject(branchId));
			if (branch != null) {
				// When would this be null?
				session.flush("persistentobjcache", null);
				session.flushObject(branchId);
				session.flushCache(false);
				branch.fetch(null);
				if (this.antecedentPermitDelta.revoke(branch)) {
					branch.save();
				}
			}
		}

		return true;
	}
}