/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;

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
	protected IDfDocument locateInCms(IDfSession session) throws CMSMFException, DfException {
		final String documentName = getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString();
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

	@Override
	protected boolean isValidForLoad(IDfDocument object) throws DfException {
		return super.isValidForLoad(object);
	}

	private void exportParentFolders(IDfDocument document, CmsDependencyManager dependencyManager) throws DfException,
		CMSMFException {
		IDfSession session = document.getSession();
		// The parent folders
		final int pathCount = document.getFolderIdCount();
		for (int i = 0; i < pathCount; i++) {
			IDfId folderId = document.getFolderId(i);
			IDfFolder parent = session.getFolderBySpecification(folderId.getId());
			dependencyManager.persistRelatedObject(parent);
		}
	}

	@Override
	protected void doPersistDependents(IDfDocument document, CmsDependencyManager dependencyManager)
		throws DfException, CMSMFException {
		final IDfSession session = document.getSession();
		if (document.isReference()) {
			// If this is a reference, this is all we're interested in
			exportParentFolders(document, dependencyManager);

		} else {

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

			// Export the object type
			dependencyManager.persistRelatedObject(document.getType());

			// Export the format
			dependencyManager.persistRelatedObject(document.getFormat());

			// Save filestore name
			String storageType = document.getStorageType();
			if (StringUtils.isNotBlank(storageType)) {
				RepositoryConfiguration.getRepositoryConfiguration().addFileStore(storageType);
			}

			exportParentFolders(document, dependencyManager);

			// Persist versions, but only if we're the root version
			IDfCollection versions = document.getVersions(null);
			try {
				while (versions.next()) {
					IDfId versionId = versions.getId("r_object_id");
					if (!Tools.equals(getId(), versionId.getId())) {
						IDfPersistentObject version = session.getObject(versionId);
						if (version == null) {
							// Just in case...shouldn't be needed
							continue;
						}
						IDfDocument versionDoc = IDfDocument.class.cast(version);
						// TODO: Is this the right way? Perhaps do it differently?
						dependencyManager.persistRelatedObject(versionDoc);
					}
				}
			} finally {
				DfUtils.closeQuietly(versions);
			}

			String dql = "" //
				+ "select dcs.r_object_id, dcr.page, dcr.page_modifier, dcs.rendition " //
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
						IDfId contentId = results.getId("r_object_id");
						IDfPersistentObject content = session.getObject(contentId);
						if (content == null) {
							// Impossible, but defend against it anyway
							this.log.warn(String
								.format("Missing page %d for document [%s](%s)", i, getLabel(), getId()));
							continue;
						}
						dependencyManager.persistRelatedObject(content);
					}
				} finally {
					DfUtils.closeQuietly(results);
				}
			}
		}
	}

	@Override
	public void resolveDependencies(IDfDocument object, CmsAttributeMapper mapper) throws DfException, CMSMFException {
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