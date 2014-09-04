package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

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
public class DctmGroup extends DctmObject<IDfGroup> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// Static variables used to see how many groups were created, skipped, updated
	/** Keeps track of nbr of group objects read from file during import process. */
	private static AtomicInteger grps_read = new AtomicInteger(0);
	/** Keeps track of nbr of group objects skipped due to duplicates during import process. */
	private static AtomicInteger grps_skipped = new AtomicInteger(0);
	/** Keeps track of nbr of group objects updated in CMS during import process. */
	private static AtomicInteger grps_updated = new AtomicInteger(0);
	/** Keeps track of nbr of group objects created in CMS during import process. */
	private static AtomicInteger grps_created = new AtomicInteger(0);

	/** The logger object used for logging. */
	private static Logger logger = Logger.getLogger(DctmGroup.class);

	/**
	 * Instantiates a new DctmGroup object.
	 */
	public DctmGroup() {
		super(DctmObjectType.DCTM_GROUP, IDfGroup.class);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS(IDfSession session) throws DfException, IOException {
		DctmGroup.grps_read.incrementAndGet();

		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Started creating dctm dm_group in repository");
		}

		// Begin transaction
		session.beginTrans();

		try {
			boolean doesGroupNeedUpdate = false;
			IDfPersistentObject newObject = null;
			// First check to see if the group already exist; if it does, check to see if we need to
// update it
			String groupName = getStrSingleAttrValue(DctmAttrNameConstants.GROUP_NAME);

			IDfGroup group = session.getGroup(groupName);
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

					newObject = group;
					doesGroupNeedUpdate = true;
				} else { // identical group exists, exit this method
					if (DctmGroup.logger.isEnabledFor(Level.DEBUG)) {
						DctmGroup.logger.debug("Identical group by name " + groupName
							+ " already exist in target repository.");
					}
					session.abortTrans();
					DctmGroup.grps_skipped.incrementAndGet();
					return;
				}
			} else { // group doesn't exist in repo, create one
				if (DctmGroup.logger.isEnabledFor(Level.DEBUG)) {
					DctmGroup.logger.debug("Creating group " + groupName + " in target repository.");
				}
				newObject = session.newObject(DctmTypeConstants.DM_GROUP);
				group = castPersistentObject(newObject);
			}

			// set various attributes
			setAllAttributesInCMS(group, this, false, doesGroupNeedUpdate);

			// save the group object
			group.save();
			if (doesGroupNeedUpdate) {
				DctmGroup.grps_updated.incrementAndGet();
			} else {
				DctmGroup.grps_created.incrementAndGet();
			}

			// update modify date of the group object
			updateModifyDate(group, this);

			if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
				DctmGroup.logger.info("Finished creating dctm dm_group in repository with name: " + groupName);
			}
		} catch (DfException e) {
			// Abort the transaction in case of DfException
			session.abortTrans();
			throw (e);
		}

		// Commit the transaction
		session.commitTrans();
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

	/**
	 * Gets the detailed group import report.
	 *
	 * @return the detailed group import report
	 */
	public static String getDetailedGroupImportReport() {
		StringBuffer importReport = new StringBuffer();
		importReport.append("\nNo. of group objects read from file: " + DctmGroup.grps_read + ".");
		importReport.append("\nNo. of group objects skipped due to duplicates: " + DctmGroup.grps_skipped + ".");
		importReport.append("\nNo. of group objects updated: " + DctmGroup.grps_updated + ".");
		importReport.append("\nNo. of group objects created: " + DctmGroup.grps_created + ".");

		return importReport.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject)
	 */
	@Override
	protected DctmGroup doGetFromCMS(IDfGroup group) throws CMSMFException {
		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Started getting dctm dm_group from repository");
		}
		IDfSession session = group.getSession();
		String groupID = "";
		try {
			groupID = group.getObjectId().getId();
			String groupName = group.getGroupName();
			// Check if this group has already been exported, if not, add to processed list
			if (!DuplicateChecker.getDuplicateChecker().isGroupProcessed(groupID, true)) {

				// First export all of the child groups recursively
				exportChildGroups(session, groupName);

				// Export all of the child users
				exportChildUsers(group);

				DctmGroup dctmGroup = new DctmGroup();
				dctmGroup.getAllAttributesFromCMS(group, groupID);

				// Export other supporting objects
				exportSupportingObjects(group);

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
		IDfSession session = groupObj.getSession();
		String groupAdmin = groupObj.getGroupAdmin();
		DctmObjectExportHelper.serializeUserOrGroupByName(session, groupAdmin);

		String groupOwner = groupObj.getOwnerName();
		DctmObjectExportHelper.serializeUserOrGroupByName(session, groupOwner);
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
		IDfSession session = parentGroup.getSession();
		String groupName = "";
		try {
			if (parentGroup != null) {
				groupName = parentGroup.getGroupName();
				// Process all of the child users
				IDfCollection childUsersNames = parentGroup.getUsersNames();
				while (childUsersNames.next()) {
					String childUserName = childUsersNames.getString(DctmAttrNameConstants.USERS_NAMES);
					try {
						DctmObjectExportHelper.serializeUserByName(session, childUserName);
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
	private void exportChildGroups(IDfSession session, String groupName) throws CMSMFException {
		if (DctmGroup.logger.isEnabledFor(Level.INFO)) {
			DctmGroup.logger.info("Started serializing child groups for dm_group with name: " + groupName);
		}
		try {
			IDfGroup parentGroup = session.getGroup(groupName);

			if (parentGroup != null) {
				String parentGroupID = parentGroup.getObjectId().getId();

				// Process all of the child groups first
				IDfCollection childGroupsNames = parentGroup.getGroupsNames();
				while (childGroupsNames.next()) {
					String childGroupName = childGroupsNames.getString(DctmAttrNameConstants.GROUPS_NAMES);
					IDfGroup childGroup = session.getGroup(childGroupName);
					if (childGroup != null) {
						String childGroupID = childGroup.getObjectId().getId();
						// Go Depth first, Process Child groups recursively if not processed yet
						// But do not add the group to processed list yet
						if (!DuplicateChecker.getDuplicateChecker().isGroupProcessed(childGroupID, false)) {
							exportChildGroups(session, childGroupName);
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
					dctmGroup.getAllAttributesFromCMS(parentGroup, parentGroupID);
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
