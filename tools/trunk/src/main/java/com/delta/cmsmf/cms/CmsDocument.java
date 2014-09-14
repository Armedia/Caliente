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
		super(CmsObjectType.DOCUMENT, IDfDocument.class);
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

	@Override
	protected void doPersistDependencies(IDfDocument document, CmsDependencyManager dependencyManager)
		throws DfException, CMSMFException {
		final IDfSession session = document.getSession();
		if (!document.isReference()) {
			String owner = CmsMappingUtils.resolveSpecialUser(session, document.getOwnerName());
			if (!CmsMappingUtils.isSpecialUserSubstitution(owner)) {
				IDfUser user = session.getUser(document.getOwnerName());
				if (user != null) {
					dependencyManager.persistDependency(user);
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
				dependencyManager.persistDependency(obj);
			}

			// Export the object type
			dependencyManager.persistDependency(document.getType());

			// Export the format
			dependencyManager.persistDependency(document.getFormat());

			// Save filestore name
			String storageType = document.getStorageType();
			if (StringUtils.isNotBlank(storageType)) {
				RepositoryConfiguration.getRepositoryConfiguration().addFileStore(storageType);
			}
		}

		// The parent folders
		final int pathCount = document.getFolderIdCount();
		for (int i = 0; i < pathCount; i++) {
			IDfId folderId = document.getFolderId(i);
			IDfFolder parent = session.getFolderBySpecification(folderId.getId());
			dependencyManager.persistDependency(parent);
		}

		if (!document.isReference()) {
			// Persist versions, but only if we're the root version
			// TODO: Is this the proper way to tell if an object is the root of the version tree?
			CmsAttribute antecedent = getAttribute(CmsAttributes.I_ANTECEDENT_ID);
			if ((antecedent == null) || antecedent.getValue().asId().isNull()) {
				// No antecedent, so we're a root object, so we can get our versions
				String chronicleId = document.getChronicleId().getId();
				String dql = String
					.format(
						"select distinct r_object_id, r_creation_date from dm_sysobject_s where i_chronicle_id = '%s' order by r_creation_date",
						chronicleId);
				IDfCollection results = DfUtils.executeQuery(session, dql);
				try {
					while (results.next()) {
						IDfId versionId = results.getId("r_object_id");
						if (Tools.equals(getId(), versionId.getId())) {
							// Do not even try to loop on ourselves, or a null ID
							continue;
						}
						IDfPersistentObject version = session.getObject(versionId);
						if (version == null) {
							// Just in case...shouldn't be needed
							continue;
						}
						dependencyManager.persistDependency(version);
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