package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.AbstractCMSMFMain;
import com.delta.cmsmf.mainEngine.DctmObjectExportHelper;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.runtime.DuplicateChecker;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

/**
 * The DctmFolder class contains methods to export/import dm_folder type (or its subtype) of objects
 * from/to
 * Documentum CMS. It also contains methods to export any supporting objects that are needed to
 * replicate a
 * dm_folder object in target repository.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmFolder extends DctmObject {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// Static variables used to see how many folders were created, skipped, updated
	/** Keeps track of nbr of folder objects read from file during import process. */
	private static AtomicInteger fldrs_read = new AtomicInteger(0);
	/** Keeps track of nbr of folder objects skipped due to duplicates during import process. */
	private static AtomicInteger fldrs_skipped = new AtomicInteger(0);
	/** Keeps track of nbr of folder objects updated in CMS during import process. */
	private static AtomicInteger fldrs_updated = new AtomicInteger(0);
	/** Keeps track of nbr of folder objects created in CMS during import process. */
	private static AtomicInteger fldrs_created = new AtomicInteger(0);

	/** The logger object used for logging. */
	private static Logger logger = Logger.getLogger(DctmFolder.class);

	/**
	 * The isThisATest is used for testing purposes. The value for this attribute is set from
	 * properties file.
	 * If the value is true, the documents and folders are created in /Replications cabinet.
	 */
	private static boolean isThisATest = AbstractCMSMFMain.getInstance().isTestMode();

	/**
	 * Instantiates a new dctm folder.
	 */
	public DctmFolder() {
		super();
		// set dctmObjectType to dctm_folder
		this.dctmObjectType = DctmObjectTypesEnum.DCTM_FOLDER;
	}

	/**
	 * Instantiates a new dctm folder with new CMS session.
	 *
	 * @param dctmSession
	 *            the existing documentum CMS session
	 */
	public DctmFolder(IDfSession dctmSession) {
		super(dctmSession);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.repoSync.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS() throws DfException, IOException {
		DctmFolder.fldrs_read.incrementAndGet();

		if (DctmFolder.logger.isEnabledFor(Level.INFO)) {
			DctmFolder.logger.info("Started creating dctm dm_folder in repository");
		}

		// Begin transaction
		this.dctmSession.beginTrans();

		try {
			// First check to see if the folder already exist; if it does check to see if we need to
// update it
			List<Object> rFolderPathList = findAttribute(DctmAttrNameConstants.R_FOLDER_PATH).getRepeatingValues();
			String rFldrPathStr = (String) rFolderPathList.get(0);

			IDfSysObject cmsFldrObjet = null;
			String objectType = getStrSingleAttrValue(DctmAttrNameConstants.R_OBJECT_TYPE);
			if (DctmFolder.isThisATest) { // for testing purposes, we are creating everything under
// /Replications cabinet
				rFldrPathStr = "/Replications" + rFldrPathStr;
				// cabinet
				if (objectType.equals(DctmTypeConstants.DM_CABINET)) {
					objectType = DctmTypeConstants.DM_FOLDER;
				}
				// remove is_private attribute
				getAttrMap().remove(DctmAttrNameConstants.IS_PRIVATE);
			}

			boolean doesFolderNeedUpdate = false;
			boolean doesFldrNameEndsWithBlank = false;
			boolean permitChangedFlag = false;
			int curPermit = 0;

			IDfFolder fldr = this.dctmSession.getFolderByPath(rFldrPathStr);
			if (fldr != null) { // we found the folder
				objectType = fldr.getTypeName();
				Date modifyDate = fldr.getModifyDate().getDate();
				if (!modifyDate.equals(findAttribute(DctmAttrNameConstants.R_MODIFY_DATE).getSingleValue())) {
					// we need to update the folder
					if (DctmFolder.logger.isEnabledFor(Level.DEBUG)) {
						DctmFolder.logger.debug("Folder/cabinet by path " + rFldrPathStr
							+ " already exist in target repository but needs to be updated.");
					}
					doesFolderNeedUpdate = true;
					cmsFldrObjet = fldr;
					// If updating an existing folder object, make sure that you have write
// permissions.
					// If you don't, grant it. Reset it later on.
					curPermit = cmsFldrObjet.getPermit();
					if (curPermit < IDfACL.DF_PERMIT_WRITE) {
						// Grant write permission and save the object
						cmsFldrObjet.grant(this.dctmSession.getLoginUserName(), IDfACL.DF_PERMIT_WRITE, null);
						cmsFldrObjet.save();
						permitChangedFlag = true;
					}
				} else { // identical folder exists, exit this method
					if (DctmFolder.logger.isEnabledFor(Level.DEBUG)) {
						DctmFolder.logger.debug("Identical folder/cabinet by path " + rFldrPathStr
							+ " already exist in target repository.");
					}
					this.dctmSession.abortTrans();
					DctmFolder.fldrs_skipped.incrementAndGet();
					return;
				}
			} else { // folder doesn't exist in repo, create one
				objectType = getStrSingleAttrValue(DctmAttrNameConstants.R_OBJECT_TYPE);
				if (DctmFolder.logger.isEnabledFor(Level.DEBUG)) {
					DctmFolder.logger.debug("Creating folder/cabinet " + rFldrPathStr
						+ " in target repository with object type " + objectType + ".");
				}
				cmsFldrObjet = (IDfSysObject) this.dctmSession.newObject(objectType);
			}

			// Check to see if the object name contains trailing blanks, if it does, trim the object
// name and
			// create the folder object. DFC throws an exception if you try to create an folder with
// spaces at
			// the end. Update the object name later on when system attributes are updated. If you
// are
			// updating an existing folder object, no need to check this condition
			String origFldrName = getStrSingleAttrValue(DctmAttrNameConstants.OBJECT_NAME);
			if (fldr == null) {
				String modifiedFldrName = origFldrName.trim();
				if (!(modifiedFldrName.equals(origFldrName))) {
					findAttribute(DctmAttrNameConstants.OBJECT_NAME).setSingleValue(modifiedFldrName);
					doesFldrNameEndsWithBlank = true;
				}
			}
			// set various attributes
			setAllAttributesInCMS(cmsFldrObjet, this, false, doesFolderNeedUpdate);
			List<String> linkedUnLinkedParentIDs = null;
			List<restoreOldACLInfo> restoreACLObjectList = new ArrayList<restoreOldACLInfo>();
			if (!objectType.equals(DctmTypeConstants.DM_CABINET)) {
				// Remove existing links and then Link the folder appropriately
				linkedUnLinkedParentIDs = DctmObject.removeAllLinks(cmsFldrObjet);

				// Find where to create the folder
				for (Object rFldrPath : rFolderPathList) {
					String rFldrPathStr2 = (String) rFldrPath;
					String parentFldrPath = rFldrPathStr2.substring(0, rFldrPathStr2.lastIndexOf("/"));

					if (DctmFolder.isThisATest) {
						parentFldrPath = "/Replications" + parentFldrPath;
					}
					// Try to link the folder to parent, if the folder is already linked to the
// specified
					// parent, DFC will throw DfException with MessageID
// DM_SYSOBJECT_E_ALREADY_LINKED. If it
					// does throw this error, catch it and ignore it, if exception is other than
// that, throw
					// it.
					try {
						cmsFldrObjet.link(parentFldrPath);
						// Add the object id of parent folders where we are trying to link the
// object in the
						// list
						// which we need to check the permissions
						IDfFolder parentFldr = this.dctmSession.getFolderByPath(parentFldrPath);
						if (!linkedUnLinkedParentIDs.contains(parentFldr.getObjectId().getId())) {
							linkedUnLinkedParentIDs.add(parentFldr.getObjectId().getId());
						}
					} catch (DfException dfe) {
						if (dfe.getMessageId().equals(CMSMFAppConstants.DM_SYSOBJECT_E_ALREADY_LINKED_MESSAGE_ID)) {
							if (DctmFolder.logger.isEnabledFor(Level.INFO)) {
								DctmFolder.logger.debug("Sysobject already Linked error handled");
							}
						} else {
							throw (dfe);
						}
					}
				}

				// Check to see if we have write permission on parent folders
				// Only for dm_folders and not for dm_cabinets
				for (String parentFldrID : linkedUnLinkedParentIDs) {
					// Check the permit on all of the parent folders from where you are unlinking or
// linking.
					IDfSysObject parentFldr = (IDfSysObject) this.dctmSession.getObject(new DfId(parentFldrID));

					// if you do not have write permit, add write permit. This will assign internal
// acl to the
					// parent folder. Keep track of the old acl of the parent folder and later on
// restore this
					// acl.
					if (parentFldr.getPermit() < IDfACL.DF_PERMIT_WRITE) {
						// Flush and ReFatch the parent folder
						this.dctmSession.flush("persistentobjcache", null);
						this.dctmSession.flushObject(parentFldr.getObjectId());
						this.dctmSession.flushCache(false);
						parentFldr = (IDfSysObject) this.dctmSession.getObject(new DfId(parentFldrID));
						parentFldr.fetch(null);
						IDfACL parentFldrACL = parentFldr.getACL();
						String aclName = parentFldrACL.getObjectName();
						String aclDomain = parentFldrACL.getDomain();
						int vStamp = parentFldr.getVStamp();
						if (DctmFolder.logger.isEnabledFor(Level.DEBUG)) {
							DctmFolder.logger.debug("Permission for parent folder " + parentFldr.getObjectId().getId()
								+ " needs to be modified. Current info about parent folder is: acl name: " + aclName
								+ " acl domain: " + aclDomain + " vStamp: " + vStamp);
						}
						parentFldr.grant(this.dctmSession.getLoginUserName(), IDfACL.DF_PERMIT_WRITE, null);
						parentFldr.save();
						restoreACLObjectList.add(new restoreOldACLInfo(aclName, aclDomain, parentFldr.getObjectId()
							.getId(), vStamp));
					}
				}
			}

			if (!doesFolderNeedUpdate) { // Creating new folder object
				// Save the folder object if creating a new one
				cmsFldrObjet.save();
				// Also check if we need to update the folder name
				if (doesFldrNameEndsWithBlank) {
					curPermit = cmsFldrObjet.getPermit();
					// Update the folder name but first check for the write permission
					if (curPermit < IDfACL.DF_PERMIT_WRITE) {
						// Grant write permission and save the object
						cmsFldrObjet.grant(this.dctmSession.getLoginUserName(), IDfACL.DF_PERMIT_WRITE, null);
						cmsFldrObjet.save();
						permitChangedFlag = true;
					}
					// set the object name and save
					cmsFldrObjet.setObjectName(origFldrName);
					cmsFldrObjet.save();
					// Revert back the permission on the system object and save
					if (permitChangedFlag) {
						cmsFldrObjet.grant(this.dctmSession.getLoginUserName(), curPermit, null);
						cmsFldrObjet.save();
					}
				}

				DctmFolder.fldrs_created.incrementAndGet();
			} else { // Updating existing folder object
				// save the folder object
				cmsFldrObjet.save();
				// Revert back the permission on the folder if it was modified previously
				if (permitChangedFlag) {
					cmsFldrObjet.grant(this.dctmSession.getLoginUserName(), curPermit, null);
					cmsFldrObjet.save();
				}
				DctmFolder.fldrs_updated.incrementAndGet();
			}

			// Update system attributes like r_creator_name, r_modifier, r_creation_date etc.
			updateSystemAttributes(cmsFldrObjet, this);

			// Restore ACL of any parent folders that was changed during the import process.
			restoreACLOfParentFolders(restoreACLObjectList);

			if (DctmFolder.logger.isEnabledFor(Level.INFO)) {
				DctmFolder.logger.info("Finished creating dctm dm_folder in repository with path: " + rFldrPathStr);
			}
		} catch (DfException e) {
			// Abort the transaction in case of DfException
			this.dctmSession.abortTrans();
			throw (e);
		}

		// Commit the transaction
		this.dctmSession.commitTrans();
	}

	/**
	 * Prints the import report detailing how many folder objects were read, updated, created,
	 * skipped during
	 * the import process.
	 */
	public static void printImportReport() {
		DctmFolder.logger.info("No. of folder objects read from file: " + DctmFolder.fldrs_read);
		DctmFolder.logger.info("No. of folder objects skipped due to duplicates: " + DctmFolder.fldrs_skipped);
		DctmFolder.logger.info("No. of folder objects updated: " + DctmFolder.fldrs_updated);
		DctmFolder.logger.info("No. of folder objects created: " + DctmFolder.fldrs_created);
	}

	/**
	 * Gets the detailed folder import report.
	 *
	 * @return the detailed folder import report
	 */
	public static String getDetailedFolderImportReport() {
		StringBuffer importReport = new StringBuffer();
		importReport.append("\nNo. of folder objects read from file: " + DctmFolder.fldrs_read + ".");
		importReport.append("\nNo. of folder objects skipped due to duplicates: " + DctmFolder.fldrs_skipped + ".");
		importReport.append("\nNo. of folder objects updated: " + DctmFolder.fldrs_updated + ".");
		importReport.append("\nNo. of folder objects created: " + DctmFolder.fldrs_created + ".");

		return importReport.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject)
	 */
	@Override
	public DctmObject getFromCMS(IDfPersistentObject prsstntObj) throws CMSMFException {
		if (DctmFolder.logger.isEnabledFor(Level.INFO)) {
			DctmFolder.logger.info("Started getting dctm dm_folder and parent folders from repository");
		}

		String fldrID = "";
		try {
			fldrID = prsstntObj.getObjectId().getId();
			// Check if this folder has already been exported, but do not add it to the processed
// list yet
			if (!DuplicateChecker.getDuplicateChecker().isFolderProcessed(fldrID, false)) {

				// First export the parent folders/cabinets
				getParentDctmFolders((IDfSysObject) prsstntObj);

				// Export other supporting objects
				exportSupportingObjects((IDfFolder) prsstntObj);

				DctmFolder dctmFolder = new DctmFolder();
				getAllAttributesFromCMS(dctmFolder, prsstntObj, fldrID);

				// Update ACL Domain attribute value if needed
				// No need to do this here anymore, it is handled in getAllAttributesFromCMS()
// itself.
				// updateACLDomainAttribute(dctmFolder);

				// Check again if the folder has been exported, if it is, return null
				if (!DuplicateChecker.getDuplicateChecker().isFolderProcessed(fldrID, true)) {
					return dctmFolder;
				} else {
					return null;
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't locate folder in repository with id: " + fldrID, e));
		}
		if (DctmFolder.logger.isEnabledFor(Level.INFO)) {
			DctmFolder.logger.info("Finished getting dctm dm_folder and parent folders from repository with id: "
				+ fldrID);
		}

		return null;

	}

	/**
	 * Exports supporting objects of this folder object. It exports folder owner, acl, object type
	 * etc.
	 *
	 * @param fldrObj
	 *            the fldr obj
	 * @throws CMSMFException
	 *             the cMSMF exception
	 * @throws DfException
	 *             the df exception
	 */
	private void exportSupportingObjects(IDfFolder fldrObj) throws CMSMFException, DfException {
		// Export folder owner
		String owner = fldrObj.getOwnerName();
		DctmObjectExportHelper.serializeUserOrGroupByName(this.dctmSession, owner);

		// Export folder group name
		String groupName = fldrObj.getGroupName();
		DctmObjectExportHelper.serializeUserOrGroupByName(this.dctmSession, groupName);

		// Export the acl
		IDfACL acl = fldrObj.getACL();
		DctmObjectExportHelper.serializeACL(this.dctmSession, acl);

		// Export the object type
		IDfType objType = fldrObj.getType();
		DctmObjectExportHelper.serializeType(this.dctmSession, objType);

		// Save filestore name
		String aStorageType = fldrObj.getStorageType();
		if (StringUtils.isNotBlank(aStorageType)) {
			RepositoryConfiguration.getRepositoryConfiguration().addFileStore(aStorageType);
		}
	}

	/**
	 * Exports parent folders of given folder object.
	 *
	 * @param sysObj
	 *            the DFC sysobject that refers to folder object whose parent folders need to be
	 *            exported
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private void getParentDctmFolders(IDfSysObject sysObj) throws CMSMFException {
		if (DctmFolder.logger.isEnabledFor(Level.INFO)) {
			DctmFolder.logger.info("Started serializing parent folders for dm_folder object");
		}

		// look up r_folder_path values and start creating objetcs from left to
		// right in r_folder_path starting with cabinets
		String fldrID = null;
		IDfFolder fldr = (IDfFolder) sysObj;
		try {
			fldrID = fldr.getObjectId().getId();
			for (int i = 0; i < fldr.getFolderPathCount(); i++) {
				String fldrPath = fldr.getFolderPath(i);
				if (DctmFolder.logger.isEnabledFor(Level.DEBUG)) {
					DctmFolder.logger.debug("r_folder_path[" + i + "] = " + fldrPath);
				}
				String parentFldrPath = fldrPath.substring(0, fldrPath.lastIndexOf("/"));
				if (StringUtils.isNotBlank(parentFldrPath)) {
					DctmObjectExportHelper.serializeFolderByPath(this.dctmSession, parentFldrPath);
				}
				/*
				 * String pathSep = "/";
				 *
				 * if (fldrPath.startsWith(pathSep)) { fldrPath = fldrPath.substring(1); } String[] fldrs =
				 * fldrPath.split(pathSep); StringBuffer curFldrPath = new StringBuffer(); for (int j = 0; j <
				 * fldrs.length - 1; j++) { curFldrPath.append(pathSep + fldrs[j]); if
				 * (logger.isEnabledFor(Level.DEBUG)) { logger.debug("Currently processing folder: " +
				 * curFldrPath); } IDfFolder ancestorFldr =
				 * dctmSession.getFolderByPath(curFldrPath.toString()); if (ancestorFldr != null) { // Check
				 * if this folder has already been serialized, but do not add it to the // processed list yet
				 * String ancestorFldrID = ancestorFldr.getObjectId().getId(); if
				 * (!DuplicateChecker.getDuplicateChecker().isFolderProcessed(ancestorFldrID, false)) { //
				 * Export other supporting objects exportSupportingObjects(ancestorFldr);
				 *
				 * DctmFolder dctmFolder = new DctmFolder(); getAllAttributesFromCMS(dctmFolder, ancestorFldr,
				 * ancestorFldrID); // Update ACL Domain attribute value // No need to do this here anymore,
				 * it is handled in getAllAttributesFromCMS() // itself. //
				 * updateACLDomainAttribute(dctmFolder); if
				 * (!DuplicateChecker.getDuplicateChecker().isFolderProcessed(ancestorFldrID, true)) {
				 * DctmObjectWriter.writeBinaryObject(dctmFolder); if (logger.isEnabledFor(Level.DEBUG)) {
				 * logger.debug("Folder object written to filesystem!"); } } } else { if
				 * (logger.isEnabledFor(Level.DEBUG)) {
				 * logger.debug("Skipping serializing folder since it is already been processed before: " +
				 * curFldrPath); } } } else { // Break out of the loop if can't locate the folder/cabinet
				 * break; } }
				 */
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve all parent folders for dctm folder with id: " + fldrID, e));
		}
// catch (IOException e) {
// throw (new
// CMSMFException("Couldn't serialize all parent folders to filesystem for dctm folder with id: "
// + fldrID, e));
// }
		if (DctmFolder.logger.isEnabledFor(Level.INFO)) {
			DctmFolder.logger.info("Finished serializing parent folders for dm_folder object with id: " + fldrID);
		}
	}

}
