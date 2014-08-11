package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.CMSMFMain;
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
	public static int fldrs_read = 0;
	/** Keeps track of nbr of folder objects skipped due to duplicates during import process. */
	public static int fldrs_skipped = 0;
	/** Keeps track of nbr of folder objects updated in CMS during import process. */
	public static int fldrs_updated = 0;
	/** Keeps track of nbr of folder objects created in CMS during import process. */
	public static int fldrs_created = 0;

	/** The logger object used for logging. */
	private static Logger logger = Logger.getLogger(DctmFolder.class);

	/**
	 * The isThisATest is used for testing purposes. The value for this attribute is set from
	 * properties file.
	 * If the value is true, the documents and folders are created in /Replications cabinet.
	 */
	private static boolean isThisATest = CMSMFMain.getInstance().isTestMode();

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
		DctmFolder.fldrs_read++;

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

			if (DctmFolder.isThisATest) { // for testing purposes, we are creating everything under
// /Replications cabinet
				rFldrPathStr = "/Replications" + rFldrPathStr;
			}

			IDfSysObject sysObject = null;
			String objectType = "";

			boolean doesFolderNeedUpdate = false;

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
					sysObject = fldr;
				} else { // identical folder exists, exit this method
					if (DctmFolder.logger.isEnabledFor(Level.DEBUG)) {
						DctmFolder.logger.debug("Identical folder/cabinet by path " + rFldrPathStr
							+ " already exist in target repository.");
					}
					this.dctmSession.abortTrans();
					DctmFolder.fldrs_skipped++;
					return;
				}
			} else { // folder doesn't exist in repo, create one
				objectType = getStrSingleAttrValue(DctmAttrNameConstants.R_OBJECT_TYPE);
				if (DctmFolder.isThisATest) { // for testing purpose, we are creating everything
// under
// /Replications
					// cabinet
					if (objectType.equals(DctmTypeConstants.DM_CABINET)) {
						objectType = DctmTypeConstants.DM_FOLDER;
					}
					// remove is_private attribute
					getAttrMap().remove(DctmAttrNameConstants.IS_PRIVATE);
				}
				if (DctmFolder.logger.isEnabledFor(Level.DEBUG)) {
					DctmFolder.logger.debug("Creating folder/cabinet " + rFldrPathStr
						+ " in target repository with object type " + objectType + ".");
				}
				sysObject = (IDfSysObject) this.dctmSession.newObject(objectType);
			}

			// set various attributes
			setAllAttributesInCMS(sysObject, this, false, doesFolderNeedUpdate);

			if (!objectType.equals(DctmTypeConstants.DM_CABINET)) {
				// Remove existing links and then Link the folder appropriately
				DctmObject.removeAllLinks(sysObject);

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
						sysObject.link(parentFldrPath);
					} catch (DfException dfe) {
						if (dfe.getMessageId().equals(CMSMFAppConstants.DM_SYSOBJECT_E_ALREADY_LINKED_MESSAGE_ID)) {
							if (DctmFolder.logger.isEnabledFor(Level.DEBUG)) {
								DctmFolder.logger.debug("Sysobject already Linked error ignored");
							}
							System.out.println("Already Linked error ignored");
						} else {
							throw (dfe);
						}
					}
				}
			}
			// Save the folder object
			sysObject.save();
			if (doesFolderNeedUpdate) {
				DctmFolder.fldrs_updated++;
			} else {
				DctmFolder.fldrs_created++;
			}
			updateSystemAttributes(sysObject, this);
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
