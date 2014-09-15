/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsDocument extends CmsObject<IDfDocument> {

	private static final String TARGET_PATHS = "targetPaths";
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

	public CmsDocument() {
		super(IDfDocument.class);
		CmsDocument.initHandlers();
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
		return String.format("%s/%s", path, document.getObjectName());
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfDocument document) throws DfException {
		CmsProperty paths = new CmsProperty(CmsDocument.TARGET_PATHS, CmsDataType.DF_STRING, true);
		final IDfSession session = document.getSession();
		for (IDfValue folderId : getAttribute(CmsAttributes.I_FOLDER_ID)) {
			IDfFolder parent = session.getFolderBySpecification(folderId.asId().getId());
			if (parent == null) { throw new DfException(String.format(
				"Document [%s](%s) references non-existent folder [%s]", getLabel(), getId(), folderId.asString())); }
			int pathCount = parent.getFolderPathCount();
			for (int i = 0; i < pathCount; i++) {
				paths.addValue(DfValueFactory.newStringValue(parent.getFolderPath(i)));
			}
		}
		properties.add(paths);
	}

	@Override
	protected IDfDocument locateInCms(CmsTransferContext ctx) throws CMSMFException, DfException {
		final String documentName = getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString();
		final IDfSession session = ctx.getSession();
		IDfDocument existing = null;
		String existingPath = null;
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
	protected boolean isValidForLoad(IDfDocument document) throws DfException {
		return super.isValidForLoad(document);
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
		if (!document.isReference()) {

			// This isn't a reference so let's do the whole shebang

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
	}

	@Override
	public void resolveDependencies(IDfDocument object, CmsTransferContext ctx) throws DfException, CMSMFException {
	}

	@Override
	protected boolean isVersionable(IDfDocument object) throws DfException {
		return true;
	}

	@Override
	protected void prepareForConstruction(IDfDocument object, boolean newObject) throws DfException {
	}

	@Override
	protected void finalizeConstruction(IDfDocument object, boolean newObject) throws DfException {
	}

	@Override
	protected boolean postConstruction(IDfDocument object, boolean newObject) throws DfException {
		return super.postConstruction(object, newObject);
	}

	@Override
	protected boolean cleanupAfterSave(IDfDocument object, boolean newObject) throws DfException {
		return super.cleanupAfterSave(object, newObject);
	}
}