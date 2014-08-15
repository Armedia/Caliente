package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.DctmObjectExportHelper;
import com.delta.cmsmf.runtime.DuplicateChecker;
import com.delta.cmsmf.serialization.DctmObjectWriter;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * The DctmGroup class contains methods to export/import dm_group type of objects from/to
 * Documentum CMS. It also contains methods to export any supporting objects that
 * are needed to replicate a dm_group object in target repository.
 * <p>
 * <b> NOTE: we are not handling aliases currently. </b>
 * <p>
 * <b> NOTE: For every group in documentum cms, there is a corresponding dm_user object; We are not
 * exporting this user object currently. </b>
 * 
 * @author Shridev Makim 6/15/2010
 */
public class DctmGroup extends DctmObject {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// Static variables used to see how many groups were created, skipped, updated
	/** Keeps track of nbr of group objects read from file during import process. */
	public static int grps_read = 0;
	/** Keeps track of nbr of group objects skipped due to duplicates during import process. */
	public static int grps_skipped = 0;
	/** Keeps track of nbr of group objects updated in CMS during import process. */
	public static int grps_updated = 0;
	/** Keeps track of nbr of group objects created in CMS during import process. */
	public static int grps_created = 0;

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(DctmGroup.class);

	/**
	 * Instantiates a new DctmGroup object.
	 */
	public DctmGroup() {
		super();
		// set dctmObjectType to dctm_group
		this.dctmObjectType = DctmObjectTypesEnum.DCTM_GROUP;
	}

	/**
	 * Instantiates a new DctmGroup object with new CMS session.
	 * 
	 * @param dctmSession
	 *            the existing documentum CMS session
	 */
	public DctmGroup(IDfSession dctmSession) {
		super(dctmSession);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS() throws DfException, IOException {
		DctmGroup.grps_read++;

		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Started creating dctm dm_group in repository");
		}

		// Begin transaction
		this.dctmSession.beginTrans();

		try {
			boolean doesGroupNeedUpdate = false;
			IDfPersistentObject prsstntObj = null;
			// First check to see if the group already exist; if it does, check to see if we need to
// update it
			String groupName = getStrSingleAttrValue(DctmAttrNameConstants.GROUP_NAME);

			IDfGroup group = this.dctmSession.getGroup(groupName);
			if (group != null) { // we found existing group
				Date curGrpModifyDate = group.getModifyDate().getDate();
				if (!curGrpModifyDate.equals(findAttribute(DctmAttrNameConstants.R_MODIFY_DATE).getSingleValue())) {
					// we need to update the group
					if (DctmGroup.logger.isEnabledFor(Level.DEBUG)) {
						DctmGroup.logger.debug("Group by name " + groupName
							+ " already exist in target repository but needs to be updated.");
					}

					// NOTE Remove the group_name attribute from attribute map to avoid following
// error
					// [DM_GROUP_E_UNABLE_TO_SAVE_EXISTING] error:
					// "Cannot save group %s because a group already exists with the same name"
					removeAttribute(DctmAttrNameConstants.GROUP_NAME);

					prsstntObj = group;
					doesGroupNeedUpdate = true;
				} else { // identical group exists, exit this method
					if (DctmGroup.logger.isEnabledFor(Level.DEBUG)) {
						DctmGroup.logger.debug("Identical group by name " + groupName
							+ " already exist in target repository.");
					}
					this.dctmSession.abortTrans();
					DctmGroup.grps_skipped++;
					return;
				}
			} else { // group doesn't exist in repo, create one
				if (DctmGroup.logger.isEnabledFor(Level.DEBUG)) {
					DctmGroup.logger.debug("Creating group " + groupName + " in target repository.");
				}
				prsstntObj = this.dctmSession.newObject(DctmTypeConstants.DM_GROUP);
			}

			// set various attributes
			setAllAttributesInCMS(prsstntObj, this, false, doesGroupNeedUpdate);

			// save the group object
			prsstntObj.save();
			if (doesGroupNeedUpdate) {
				DctmGroup.grps_updated++;
			} else {
				DctmGroup.grps_created++;
			}

			// update modify date of the group object
			updateModifyDate(prsstntObj, this);

			if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
				DctmGroup.logger.info("Finished creating dctm dm_group in repository with name: " + groupName);
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
	 * Prints the import report detailing how many group objects were read, updated, created,
	 * skipped
	 * during the import process.
	 */
	public static void printImportReport() {
		DctmGroup.logger.info("No. of group objects read from file: " + DctmGroup.grps_read);
		DctmGroup.logger.info("No. of group objects skipped due to duplicates: " + DctmGroup.grps_skipped);
		DctmGroup.logger.info("No. of group objects updated: " + DctmGroup.grps_updated);
		DctmGroup.logger.info("No. of group objects created: " + DctmGroup.grps_created);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject)
	 */
	@Override
	public DctmObject getFromCMS(IDfPersistentObject prsstntObj) throws CMSMFException {
		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Started getting dctm dm_group from repository");
		}
		String groupID = "";
		try {
			groupID = prsstntObj.getObjectId().getId();
			String groupName = ((IDfGroup) prsstntObj).getGroupName();
			// Check if this group has already been exported, if not, add to processed list
			if (!DuplicateChecker.getDuplicateChecker().isGroupProcessed(groupID, true)) {

				// First export all of the child groups recursively
				exportChildGroups(groupName);

				// Export all of the child users
				exportChildUsers((IDfGroup) prsstntObj);

				DctmGroup dctmGroup = new DctmGroup();
				getAllAttributesFromCMS(dctmGroup, prsstntObj, groupID);

				// Export other supporting objects
				exportSupportingObjects((IDfGroup) prsstntObj);

				return dctmGroup;
			} else {
				if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
					DctmGroup.logger.info("Group " + groupName + " already has been or is being exported!");
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Error retrieving group in repository with id: " + groupID, e));
		}
		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Finished getting dctm dm_group from repository with id: " + groupID);
		}

		return null;
	}

	/**
	 * Export supporting objects.
	 * 
	 * @param groupObj
	 *            the group obj
	 * @throws CMSMFException
	 *             the cMSMF exception
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private void exportSupportingObjects(IDfGroup groupObj) throws CMSMFException, DfException {
		// Export group admin and group owner
		String groupAdmin = groupObj.getGroupAdmin();
		DctmObjectExportHelper.serializeUserOrGroupByName(this.dctmSession, groupAdmin);

		String groupOwner = groupObj.getOwnerName();
		DctmObjectExportHelper.serializeUserOrGroupByName(this.dctmSession, groupOwner);
	}

	/**
	 * Exports child users of a given group object.
	 * 
	 * @param parentGroup
	 *            the parent group
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private void exportChildUsers(IDfGroup parentGroup) throws CMSMFException {
		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Started serializing child users for group");
		}
		String groupName = "";
		try {
			if (parentGroup != null) {
				groupName = parentGroup.getGroupName();
				// Process all of the child users
				IDfCollection childUsersNames = parentGroup.getUsersNames();
				while (childUsersNames.next()) {
					String childUserName = childUsersNames.getString(DctmAttrNameConstants.USERS_NAMES);
					try {
						DctmObjectExportHelper.serializeUserByName(this.dctmSession, childUserName);
					} catch (CMSMFException e) {
						// if for some reason user is not serialized, catch the exception and
// continue on
						// with next user. Log the error message
						DctmGroup.logger.warn("Could not serialize a child user of a group. User name was: "
							+ childUserName);
					}
				}
				childUsersNames.close(); // close the collection
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve all child users for dctm group with name: " + groupName, e));
		}

		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Finished serializing child users for group with name: " + groupName);
		}
	}

	/**
	 * Exports child groups of given group object.
	 * This method calls itself recursively to traverse
	 * all of the child groups before exporting itself.
	 * 
	 * @param groupName
	 *            the group name
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private void exportChildGroups(String groupName) throws CMSMFException {
		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Started serializing child groups for dm_group with name: " + groupName);
		}
		try {
			IDfGroup parentGroup = this.dctmSession.getGroup(groupName);

			if (parentGroup != null) {
				String parentGroupID = parentGroup.getObjectId().getId();

				// Process all of the child groups first
				IDfCollection childGroupsNames = parentGroup.getGroupsNames();
				while (childGroupsNames.next()) {
					String childGroupName = childGroupsNames.getString(DctmAttrNameConstants.GROUPS_NAMES);
					IDfGroup childGroup = this.dctmSession.getGroup(childGroupName);
					if (childGroup != null) {
						String childGroupID = childGroup.getObjectId().getId();
						// Go Depth first, Process Child groups recursively if not processed yet
						// But do not add the group to processed list yet
						if (!DuplicateChecker.getDuplicateChecker().isGroupProcessed(childGroupID, false)) {
							exportChildGroups(childGroupName);
						}
					}
				}

				// After all of the child groups are traversed, export the parent group
				// and add to processed list. Also export all of the child users and
				// supporting objects
				if (!DuplicateChecker.getDuplicateChecker().isGroupProcessed(parentGroupID, true)) {

					// export supporting objects
					exportSupportingObjects(parentGroup);

					// export the group object
					DctmGroup dctmGroup = new DctmGroup();
					getAllAttributesFromCMS(dctmGroup, parentGroup, parentGroupID);
					DctmObjectWriter.writeBinaryObject(dctmGroup);

					// export child users
					exportChildUsers(parentGroup);
				} else {
					if (DctmGroup.logger.isEnabledFor(Level.DEBUG)) {
						DctmGroup.logger.debug("Skipping serializing group since it is already been processed before: "
							+ groupName);
					}
				}
				childGroupsNames.close(); // close the collection
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve all child groups for dctm group with name: " + groupName, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't serialize all child groups for dctm group with name: " + groupName, e));
		}
		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Finished serializing child groups for dm_group with name: " + groupName);
		}
	}

}
