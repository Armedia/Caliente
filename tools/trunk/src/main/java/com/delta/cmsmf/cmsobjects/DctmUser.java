package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.DctmObjectExportHelper;
import com.delta.cmsmf.mainEngine.DctmObjectImportHelper;
import com.delta.cmsmf.properties.CMSMFProperties;
import com.delta.cmsmf.runtime.DuplicateChecker;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;

/**
 * The DctmUser class contains methods to export/import dm_user type of objects from/to
 * Documentum CMS. It also contains methods to export any supporting objects that
 * are needed to replicate a dm_user object in target repository.
 * <p>
 * <b> NOTE: We are not handling aliases currently. </b>
 * <p>
 * <b> NOTE: During import process, if the user name is dmadmin or starts with "dm_",it will be
 * ignore and will not be created them in the repository.</b>
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmUser extends DctmObject {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// NOTE We need to figure out how to retrieve the user password and set it if needed and if it
// is at all
	// possible

	// Static variables used to see how many groups were created, skipped, updated
	/** Keeps track of nbr of user objects read from file during import process. */
	private static AtomicInteger usrs_read = new AtomicInteger(0);
	/** Keeps track of nbr of user objects skipped due to duplicates during import process. */
	private static AtomicInteger usrs_skipped = new AtomicInteger(0);
	/** Keeps track of nbr of user objects updated in CMS during import process. */
	private static AtomicInteger usrs_updated = new AtomicInteger(0);
	/** Keeps track of nbr of user objects created in CMS during import process. */
	private static AtomicInteger usrs_created = new AtomicInteger(0);

	/** The logger object used for logging. */
	private static Logger logger = Logger.getLogger(DctmUser.class);

	/** The boolean flag to see if user has internal acl. */
	private boolean doesUserHaveInternalACL;

	/**
	 * Instantiates a new DctmUser object.
	 */
	public DctmUser() {
		super();
		// set dctmObjectType to dctm_user
		this.dctmObjectType = DctmObjectType.DCTM_USER;
	}

	/**
	 * Instantiates a new DctmUser object with new CMS session.
	 *
	 * @param dctmSession
	 *            the existing documentum CMS session
	 */
	public DctmUser(IDfSession dctmSession) {
		super(dctmSession);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS() throws DfException, IOException {
		DctmUser.usrs_read.incrementAndGet();

		if (DctmUser.logger.isEnabledFor(Level.INFO)) {
			DctmUser.logger.info("Started creating dctm dm_user in repository");
		}

		// Begin transaction
		this.dctmSession.beginTrans();

		// First check to see if the user already exist; if it does check to see if we need to
// update it
		String userName;
		try {
			boolean doesUserNeedUpdate = false;
			IDfPersistentObject prsstntObj = null;
			userName = getStrSingleAttrValue(DctmAttrNameConstants.USER_NAME);

			// NOTE if the user name is dmadmin or starts with "dm_", ignore and do not create them
// in the
			// repository
			if (userName.equals("dmadmin") || userName.startsWith("dm_")) {
				if (DctmUser.logger.isEnabledFor(Level.INFO)) {
					DctmUser.logger.info("The user " + userName
						+ " was not created in cms. It appears to be a system user");
				}
				// abort the transaction and exit out
				this.dctmSession.abortTrans();
				DctmUser.usrs_skipped.incrementAndGet();
				return;
			}

			String defaultACLName = getStrSingleAttrValue(DctmAttrNameConstants.ACL_NAME);

			// If user source is inline password, reset the password equal to the value specified in
			// properties file. Password would be set to either loginid or fixed text specified in
			// properties file. For all other types of user source, clear out the password field.
			// If the value in the properties file is 'sameasloginid' set the password equal to the
			// the user_login_name
			if (getStrSingleAttrValue(DctmAttrNameConstants.USER_SOURCE).equals(
				CMSMFAppConstants.USER_SOURCE_INLINE_PASSWORD)) {
				// Default the password to the user's login name, if one hasn't been selected
				// for global use
				final String inlinePasswordValue = CMSMFProperties.DEFAULT_USER_PASSWORD
					.getString(DctmAttrNameConstants.USER_LOGIN_NAME);
				findAttribute(DctmAttrNameConstants.USER_PASSWORD).setSingleValue(inlinePasswordValue);

			} else {
				// For non inline password users, clear out the password field
				removeAttribute(DctmAttrNameConstants.USER_PASSWORD);
			}

			// NOTE for some reason, 6.5 sp2 with ldap requires that user_login_domain be set
			// workaround for [DM_USER_E_MUST_HAVE_LOGINDOMAIN] error
			if (findAttribute(DctmAttrNameConstants.USER_LOGIN_DOMAIN) == null) {
				DctmAttribute userLoginDomain = new DctmAttribute(DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE);
				userLoginDomain.setSingleValue("");
				addAttribute(DctmAttrNameConstants.USER_LOGIN_DOMAIN, userLoginDomain);
			}

			// Try to lookup the user by login name and login domain
			String userLoginName = getStrSingleAttrValue(DctmAttrNameConstants.USER_LOGIN_NAME);
			String userLoginDomain = getStrSingleAttrValue(DctmAttrNameConstants.USER_LOGIN_DOMAIN);
			if (userLoginDomain.equals("")) {
				userLoginDomain = null;
			}
			IDfUser usr = this.dctmSession.getUserByLoginName(userLoginName, userLoginDomain);

			if (usr != null) { // we found existing user
				Date curUsrModifyDate = usr.getModifyDate().getDate();
				if (!curUsrModifyDate.equals(findAttribute(DctmAttrNameConstants.R_MODIFY_DATE).getSingleValue())) {
					// we need to update the user
					if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
						DctmUser.logger.debug("User by login name " + userLoginName + " and login domain "
							+ userLoginDomain + " already exist in target repository but needs to be updated.");
					}

					// NOTE Remove the user_name attribute from attribute map to avoid following
// error if it
					// is the same as before
					// [DM_USER_E_EXISTING_USER_NAME] error:
					// "Cannot create user %s since its user_name already exists"
					if (usr.getUserName().equals(getStrSingleAttrValue(DctmAttrNameConstants.USER_NAME))) {
						if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
							DctmUser.logger
							.debug("User name will not be updated because it's same as before. Currently in target repo: "
								+ usr.getUserName());
						}
						removeAttribute(DctmAttrNameConstants.USER_NAME);
					}

					prsstntObj = usr;
					doesUserNeedUpdate = true;
				} else { // identical user exists, exit this method
					if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
						DctmUser.logger.debug("Identical user by name " + userName
							+ " already exist in target repository.");
					}
					this.dctmSession.abortTrans();
					DctmUser.usrs_skipped.incrementAndGet();
					return;
				}
			} else { // user doesn't exist in repo, create one
				if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
					DctmUser.logger.debug("Creating user " + userName + " in target repository.");
				}
				prsstntObj = this.dctmSession.newObject(DctmTypeConstants.DM_USER);
			}

			// NOTE First make sure that the default group for the user exists, if not create a one
// it will
			// be updated later on, when the groups file will be imported.
			createRequiredUserDefaults();

			// NOTE if you try to change a user home docbase by setting home_docbase attribute, dfc
// throws
			// an error that you need to use changeHomeDocbase() method of IDfUser. remove this
// attribute
			// from attribute map.
			String existingHomeDocbase = ((IDfUser) prsstntObj).getHomeDocbase();
			String newHomeDocbase = getStrSingleAttrValue(DctmAttrNameConstants.HOME_DOCBASE);
			removeAttribute(DctmAttrNameConstants.HOME_DOCBASE);
			if (doesUserNeedUpdate) {
				// If new home docbase value is not empty and if it is different from existing, set
// it
				// by calling changeHomeDocbase() method.
				if (!newHomeDocbase.equals("") && !existingHomeDocbase.equals(newHomeDocbase)) {
					((IDfUser) prsstntObj).changeHomeDocbase(newHomeDocbase, true);
				}
			}

			// set various attributes
			setAllAttributesInCMS(prsstntObj, this, false, doesUserNeedUpdate);

			// save the user object
			prsstntObj.save();
			if (doesUserNeedUpdate) {
				DctmUser.usrs_updated.incrementAndGet();
			} else {
				DctmUser.usrs_created.incrementAndGet();
			}

			// if user had internal acl, check to see if its name needs to be updated
			// If user is being updated, acl domain and acl name may have been cleared, check for
// that.
			if (this.doesUserHaveInternalACL && (findAttribute(DctmAttrNameConstants.ACL_NAME) != null)
				&& (findAttribute(DctmAttrNameConstants.ACL_DOMAIN) != null)) {
				updateInternalACL(prsstntObj, defaultACLName);
			}
			// update modify date of the user object
			updateModifyDate(prsstntObj, this);

			if (DctmUser.logger.isEnabledFor(Level.INFO)) {
				DctmUser.logger.info("Finished creating dctm dm_user in repository with name: " + userName);
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
	 * Prints the import report detailing how many user objects were read, updated, created, skipped
	 * during the import process.
	 */
	public static void printImportReport() {
		DctmUser.logger.info("No. of user objects read from file: " + DctmUser.usrs_read);
		DctmUser.logger.info("No. of user objects skipped due to duplicates: " + DctmUser.usrs_skipped);
		DctmUser.logger.info("No. of user objects updated: " + DctmUser.usrs_updated);
		DctmUser.logger.info("No. of user objects created: " + DctmUser.usrs_created);
	}

	/**
	 *
	 * Gets the detailed user import report.
	 *
	 * @return the detailed user import report
	 */
	public static String getDetailedUserImportReport() {
		StringBuffer importReport = new StringBuffer();
		importReport.append("\nNo. of user objects read from file: " + DctmUser.usrs_read + ".");
		importReport.append("\nNo. of user objects skipped due to duplicates: " + DctmUser.usrs_skipped + ".");
		importReport.append("\nNo. of user objects updated: " + DctmUser.usrs_updated + ".");
		importReport.append("\nNo. of user objects created: " + DctmUser.usrs_created + ".");

		return importReport.toString();
	}

	/**
	 * Updates internal acl of a given user object.
	 *
	 * @param prsstntObj
	 *            the persistent object that represents user object in cms.
	 * @param defaultACLName
	 *            the new default acl name
	 * @throws DfException
	 *             the df exception
	 */
	private void updateInternalACL(IDfPersistentObject prsstntObj, String defaultACLName) throws DfException {
		String curUsrDefaultACLName = ((IDfUser) prsstntObj).getACLName();

		// Check if the user internal default acl is different from what was on the file
		// if it is different, modify the current default acl and add back the new one as a default
		// acl to the user.
		if (!curUsrDefaultACLName.equals(defaultACLName)) {
			String userACLDomainName = getStrSingleAttrValue(DctmAttrNameConstants.ACL_DOMAIN);
			IDfACL curUsrDefaultACL = this.dctmSession.getACL(userACLDomainName, curUsrDefaultACLName);

			if (curUsrDefaultACL != null) {
				// IDfId newACLID = curUsrDefaultACL.saveAsNew();
				// IDfACL newDefaultACL = (IDfACL) dctmSession.getObject(newACLID);
				// newDefaultACL.setObjectName(defaultACLName);
				// newDefaultACL.setDomain(userACLDomainName);
				// newDefaultACL.setDescription(CMSMFAppConstants.CMSMF_TEMP_ACL_DESCRIPTION);
				// newDefaultACL.save();

				curUsrDefaultACL.setObjectName(defaultACLName);
				curUsrDefaultACL.setDescription(CMSMFAppConstants.CMSMF_TEMP_ACL_DESCRIPTION);
				curUsrDefaultACL.save();

				if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
					DctmUser.logger.debug("The internal default acl of the user: " + userACLDomainName
						+ " was updated successfully.");
				}
				// add new acl as default acl for the user
				((IDfUser) prsstntObj).setDefaultACLEx(userACLDomainName, defaultACLName);
				prsstntObj.save();
				// NOTE if a user default folder was created using old acl name, it will be updated
// later on
				// when folders are processed

				// NOTE destroy the old acl only needed if creating new acl object
				// curUsrDefaultACL.destroyACL(true);
			}
		}

	}

	/**
	 * Creates the required user defaults for this user.
	 * This method creates default folder, default group and default acl in repository
	 * during import process if they do not exist. These objects will be updated later on
	 * when the import process imports group, acls and folders.
	 *
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private void createRequiredUserDefaults() throws DfException {
		if (DctmUser.logger.isEnabledFor(Level.INFO)) {
			DctmUser.logger.info("Started checking if the default group and default acl for the user exists.");
		}

		// Create default folder for the user if it does not exist
		if (doesAttributeExist(DctmAttrNameConstants.DEFAULT_FOLDER)) {
			String defaultFolderNAme = getStrSingleAttrValue(DctmAttrNameConstants.DEFAULT_FOLDER);
			DctmObjectImportHelper.createFolderByPath(this.dctmSession, defaultFolderNAme);
		}

		// lookup default group for the user in repository, if it does not exist, create one
		if (doesAttributeExist(DctmAttrNameConstants.USER_GROUP_NAME)) {
			String userGroupName = getStrSingleAttrValue(DctmAttrNameConstants.USER_GROUP_NAME);
			IDfGroup userDefaultGroup = this.dctmSession.getGroup(userGroupName);
			if (userDefaultGroup == null) {
				IDfGroup defaultGroup = (IDfGroup) this.dctmSession.newObject(DctmTypeConstants.DM_GROUP);
				defaultGroup.setGroupName(userGroupName);
				defaultGroup.save();
				if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
					DctmUser.logger.debug("The default group of the user: " + userGroupName
						+ " was created successfully.");
				}
			} else {
				if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
					DctmUser.logger.debug("The default group: " + userGroupName + " for the user: " + userGroupName
						+ " already exists.");
				}
			}
		}

		// NOTE If user has an "internal" default ACL, and if that does not exist, let system create
// one
		// and then rename it. otherwise check if system acl exists and if it doesn't create one.
		String userDefaultACLName = getStrSingleAttrValue(DctmAttrNameConstants.ACL_NAME);
		String userDefaultACLDomainName = getStrSingleAttrValue(DctmAttrNameConstants.ACL_DOMAIN);
		if (this.doesUserHaveInternalACL) {
			if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
				DctmUser.logger.debug("The user's default ACL is internal ACL");
			}
			IDfACL userDefaultACL = this.dctmSession.getACL(userDefaultACLDomainName, userDefaultACLName);
			// if internal acl does not exist, let the system create one and we will remove related
			// attributes from the attribute map
			if (userDefaultACL == null) {
				if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
					DctmUser.logger
						.debug("The user's default Internal ACL does not exist and acl related attributes will be removed");
				}
				removeAttribute(DctmAttrNameConstants.ACL_NAME);
				removeAttribute(DctmAttrNameConstants.ACL_DOMAIN);
			}
		} else {
			if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
				DctmUser.logger.debug("The user's default ACL is Named (non internal) ACL");
			}

			userDefaultACLDomainName = getStrSingleAttrValue(DctmAttrNameConstants.ACL_DOMAIN);
			// userDefaultACLDomainName = DctmAttrNameConstants.DM_DBO;
			// findAttribute(DctmAttrNameConstants.ACL_DOMAIN).setSingleValue(userDefaultACLDomainName);
			IDfACL userDefaultACL = this.dctmSession.getACL(userDefaultACLDomainName, userDefaultACLName);
			if (userDefaultACL == null) {
				IDfACL defaultACL = (IDfACL) this.dctmSession.newObject(DctmTypeConstants.DM_ACL);
				defaultACL.setObjectName(userDefaultACLName);
				defaultACL.setDomain(userDefaultACLDomainName);
				defaultACL.setDescription(CMSMFAppConstants.CMSMF_TEMP_ACL_DESCRIPTION);
				defaultACL.save();
				if (DctmUser.logger.isEnabledFor(Level.DEBUG)) {
					DctmUser.logger.debug("The default acl of the user: " + userDefaultACLName
						+ " was created successfully.");
				}
			}
		}

		if (DctmUser.logger.isEnabledFor(Level.INFO)) {
			DctmUser.logger.info("Finished checking if the default group and default acl for the user exists.");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject)
	 */
	@Override
	protected DctmObject doGetFromCMS(IDfPersistentObject prsstntObj) throws CMSMFException {
		if (DctmUser.logger.isEnabledFor(Level.INFO)) {
			DctmUser.logger.info("Started getting dctm dm_user from repository");
		}

		String userID = "";
		try {
			userID = prsstntObj.getObjectId().getId();
			// Check if this user has already been exported
			if (!DuplicateChecker.getDuplicateChecker().isUserProcessed(userID)) {
				DctmUser dctmUser = new DctmUser();
				getAllAttributesFromCMS(dctmUser, prsstntObj, userID);

				// Update ACL Domain attribute value if needed
				// No need to do this here anymore, it is handled in getAllAttributesFromCMS()
// itself.
// updateACLDomainAttribute(dctmUser);

				// Check to see if user's default acl is internal or not
				IDfUser user = (IDfUser) prsstntObj;
				IDfACL userDefaultACL = prsstntObj.getSession().getACL(user.getACLDomain(), user.getACLName());
				dctmUser.doesUserHaveInternalACL = userDefaultACL.isInternal();

				// Get users default folder, group and acls.
				exportUserDefaults(dctmUser);

				return dctmUser;
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't locate folder in repository with id: " + userID, e));
		}
		if (DctmUser.logger.isEnabledFor(Level.INFO)) {
			DctmUser.logger.info("Finished getting dctm dm_user from repository with id: " + userID);
		}

		return null;
	}

	/**
	 * Exports various user defaults for a given user.
	 * This method exports default folder, default group and default acl
	 * of an user object.
	 *
	 * @param dctmUser
	 *            the dctm user
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private void exportUserDefaults(DctmUser dctmUser) throws CMSMFException {

		String userName = dctmUser.getStrSingleAttrValue(DctmAttrNameConstants.USER_NAME);

		// get user default folder if it exists in the system
		String defaultFolder = dctmUser.getStrSingleAttrValue(DctmAttrNameConstants.DEFAULT_FOLDER);
		if (StringUtils.isNotBlank(defaultFolder)) {
			try {
				DctmObjectExportHelper.serializeFolderByPath(this.dctmSession, defaultFolder);
			} catch (CMSMFException e) {

				// NOTE if for some reason the default folder does not exist in the system,
				// log it as a warning and continue on.
				DctmUser.logger.warn("Couldn't retrieve default folder object from repository for user: " + userName
					+ " folder value: " + defaultFolder, e);
				// throw (new CMSMFException(
				// "Couldn't retrieve default folder object from repository for user: "
				// + userName, e));
			}
		}

		// get user default group
		String defaultGroup = dctmUser.getStrSingleAttrValue(DctmAttrNameConstants.USER_GROUP_NAME);
		if (StringUtils.isNotBlank(defaultGroup)) {
			try {
				DctmObjectExportHelper.serializeGroupByName(this.dctmSession, defaultGroup);
			} catch (CMSMFException e) {
				throw (new CMSMFException("Couldn't retrieve default group object from repository for user: "
					+ userName + " group name: " + defaultGroup, e));
			}
		}

		String aclName = dctmUser.getStrSingleAttrValue(DctmAttrNameConstants.ACL_NAME);
		String aclDomain = dctmUser.getStrSingleAttrValue(DctmAttrNameConstants.ACL_DOMAIN);
		if (StringUtils.isNotBlank(aclName) && StringUtils.isNotBlank(aclDomain)) {
			try {
				DctmObjectExportHelper.serializeACLByName(this.dctmSession, aclName, aclDomain);
			} catch (CMSMFException e) {
				throw (new CMSMFException("Couldn't retrieve default acl object from repository for user: " + userName
					+ " acl name: " + aclName + " acl domain: " + aclDomain, e));
			}
		}
	}

}
