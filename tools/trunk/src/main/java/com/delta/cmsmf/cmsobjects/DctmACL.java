package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.datastore.DataObject;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.DctmObjectExportHelper;
import com.delta.cmsmf.runtime.DuplicateChecker;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfValue;

/**
 * The DctmACL class contains methods to export/import dm_acl type of objects from/to
 * Documentum CMS. It also contains methods to export any supporting objects that
 * are needed to replicate a dm_acl object in target repository.
 * <p>
 * <b> NOTE: we are not handling aliases currently. </b>
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmACL extends DctmObject<IDfACL> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// Static variables used to see how many ACLs were created, skipped, updated
	/** Keeps track of nbr of acl objects read from file during import process. */
	private static AtomicInteger acls_read = new AtomicInteger(0);
	/** Keeps track of nbr of acl objects skipped due to duplicates during import process. */
	private static AtomicInteger acls_skipped = new AtomicInteger(0);
	/** Keeps track of nbr of acl objects updated in CMS during import process. */
	private static AtomicInteger acls_updated = new AtomicInteger(0);
	/** Keeps track of nbr of acl objects created in CMS during import process. */
	private static AtomicInteger acls_created = new AtomicInteger(0);

	/** The logger object used for logging. */
	private static Logger logger = Logger.getLogger(DctmACL.class);

	/** The list that stores extended permission names for this ACL object. */
	private final List<String> accessorXPermitNames = new ArrayList<String>();

	/**
	 * Instantiates a new DctmACL object.
	 */
	public DctmACL() {
		super(DctmObjectType.DCTM_ACL, IDfACL.class);
	}

	@Override
	protected void doLoadFrom(DataObject dataObject) {
		DataProperty prop = dataObject.getProperty(DctmAttrNameConstants.R_ACCESSOR_XPERMIT);
		this.accessorXPermitNames.clear();
		for (IDfValue value : prop) {
			this.accessorXPermitNames.add(value.asString());
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS(IDfSession session) throws DfException, IOException {
		DctmACL.acls_read.incrementAndGet();

		if (DctmACL.logger.isEnabledFor(Level.INFO)) {
			DctmACL.logger.info("Started creating dctm dm_acl in repository");
		}

		// Begin transaction
		session.beginTrans();

		try {
			boolean doesACLNeedUpdate = false;
			IDfPersistentObject newObject = null;
			// First check to see if the ACL already exist; if it does, check to see if we need to
			// update it
			String aclName = getStrSingleAttrValue(DctmAttrNameConstants.OBJECT_NAME);

			String aclDomainName = getStrSingleAttrValue(DctmAttrNameConstants.OWNER_NAME);
			final int newVStamp = getIntSingleAttrValue(DctmAttrNameConstants.I_VSTAMP);
			IDfACL acl = session.getACL(aclDomainName, aclName);
			if (acl != null) { // we found existing acl
				int existingVStamp = acl.getVStamp();
				if (existingVStamp != newVStamp) {
					// We need to update the ACL
					if (DctmACL.logger.isEnabledFor(Level.DEBUG)) {
						DctmACL.logger.debug("ACL by name " + aclName
							+ " already exist in target repository but needs to be updated.");
					}

					newObject = acl;
					doesACLNeedUpdate = true;
				} else {
					// Identical ACL exists in the target repository, abort the transaction and
					// quit
					if (DctmACL.logger.isEnabledFor(Level.DEBUG)) {
						DctmACL.logger.debug("Identical acl " + aclName + " already exists in target repository.");
					}
					session.abortTrans();
					DctmACL.acls_skipped.incrementAndGet();
					return;
				}
			} else { // acl doesn't exist in repo, create one
				if (DctmACL.logger.isEnabledFor(Level.DEBUG)) {
					DctmACL.logger.debug("Creating acl " + aclName + " in target repository.");
				}
				newObject = session.newObject(DctmTypeConstants.DM_ACL);
				acl = castPersistentObject(newObject);
			}

			// set various attributes
			setAllAttributesInCMS(acl, this, false, doesACLNeedUpdate);

			// Set Accessor Permissions
			setAccessorPermissions(acl, doesACLNeedUpdate);

			// save the ACL object
			acl.save();
			if (doesACLNeedUpdate) {
				DctmACL.acls_updated.incrementAndGet();
			} else {
				DctmACL.acls_created.incrementAndGet();
			}

			// update vStamp of the acl object
			updateVStamp(acl, newVStamp);

			if (DctmACL.logger.isEnabledFor(Level.INFO)) {
				DctmACL.logger.info("Finished creating dctm dm_acl in repository with name: " + aclName);
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
	 * Prints the import report detailing how many acl objects were read, updated, created, skipped
	 * during the import process.
	 */
	public static void printImportReport() {
		DctmACL.logger.info("No. of ACL objects read from file: " + DctmACL.acls_read);
		DctmACL.logger.info("No. of ACL objects skipped due to duplicates: " + DctmACL.acls_skipped);
		DctmACL.logger.info("No. of ACL objects updated: " + DctmACL.acls_updated);
		DctmACL.logger.info("No. of ACL objects created: " + DctmACL.acls_created);
	}

	/**
	 * Gets the detailed acl import report.
	 *
	 * @return the detailed acl import report
	 */
	public static String getDetailedAclImportReport() {
		StringBuffer importReport = new StringBuffer();
		importReport.append("\nNo. of ACL objects read from file: " + DctmACL.acls_read + ".");
		importReport.append("\nNo. of ACL objects skipped due to duplicates: " + DctmACL.acls_skipped + ".");
		importReport.append("\nNo. of ACL objects updated: " + DctmACL.acls_updated + ".");
		importReport.append("\nNo. of ACL objects created: " + DctmACL.acls_created + ".");

		return importReport.toString();
	}

	/**
	 * Sets the accessor permissions of an acl object in the CMS.
	 *
	 * @param aclObj
	 *            the DFC ACL object for which permissions are being set
	 * @param doesACLNeedUpdate
	 *            the boolean flag set to true if an existing acl object is being updated
	 * @throws DfException
	 *             the df exception
	 */
	private void setAccessorPermissions(IDfACL aclObj, boolean doesACLNeedUpdate) throws DfException {

		// If you are updating the ACL, revoke all of the permissions first
		if (doesACLNeedUpdate) {
			IDfList permissions = aclObj.getPermissions();
			for (int i = 0; i < permissions.getCount(); i++) {
				IDfPermit permit = (IDfPermit) permissions.get(i);
				aclObj.revokePermit(permit);
			}
		}
		List<Object> rAccessorName = findAttribute(DctmAttrNameConstants.R_ACCESSOR_NAME).getRepeatingValues();
		List<Object> rAccessorPermit = findAttribute(DctmAttrNameConstants.R_ACCESSOR_PERMIT).getRepeatingValues();

		// Grant permissions one by one
		for (int i = 0; i < rAccessorName.size(); i++) {
			aclObj.grant((String) rAccessorName.get(i), (Integer) rAccessorPermit.get(i),
				this.accessorXPermitNames.get(i));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject)
	 */
	@Override
	protected DctmACL doGetFromCMS(IDfACL acl) throws CMSMFException {
		if (DctmACL.logger.isEnabledFor(Level.INFO)) {
			DctmACL.logger.info("Started getting dctm dm_acl from repository");
		}
		String aclID = "";
		try {
			aclID = acl.getObjectId().getId();
			String aclName = acl.getString(DctmAttrNameConstants.OBJECT_NAME);
			// Check if this acl has already been exported, if not, add to processed list
			if (!DuplicateChecker.getDuplicateChecker().isACLProcessed(aclID)) {

				DctmACL dctmACL = new DctmACL();
				dctmACL.getAllAttributesFromCMS(acl, aclID);

				// Update ACL Domain attribute value if needed
				// No need to do this here anymore, it is handled in getAllAttributesFromCMS()
				// itself.
				// updateACLDomainAttribute(dctmACL);

				// populate Accessor Extended Permission Names
				// NOTE Accessor Extended Permissions are stored in some type of int representation
				// of various Extended permissions. But to recreate this set in target cms, you have
				// to provide coma separated String values in Grant() method in IDfACL. So store
				// this string representation here.
				populateAccessorXPermitNames(acl);

				acl.isInternal();

				// Export other supporting objects
				exportSupportingObjects(acl);

				return dctmACL;
			} else {
				if (DctmACL.logger.isEnabledFor(Level.INFO)) {
					DctmACL.logger.info("ACL " + aclName + " already has been or is being exported!");
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Error retrieving acl in repository with id: " + aclID, e));
		}
		if (DctmACL.logger.isEnabledFor(Level.INFO)) {
			DctmACL.logger.info("Finished getting dctm dm_acl from repository with id: " + aclID);
		}

		return null;
	}

	/**
	 * Populates extended permission names to a given CMSMF DctmACL object from a given
	 * acl object in a CMS.
	 *
	 * @param dctmACL
	 *            the CMSMF DctmACL object whose extended permissions list needs to be populated
	 * @param aclObj
	 *            the acl object in CMS
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private void populateAccessorXPermitNames(IDfACL aclObj) throws DfException {
		int accessorCount = aclObj.getAccessorCount();

		if (accessorCount > 0) {
			DataProperty prop = new DataProperty(DctmAttrNameConstants.R_ACCESSOR_XPERMIT, DataType.DF_STRING);
			getDataObject().setProperty(prop);
			for (int i = 0; i < accessorCount; i++) {
				this.accessorXPermitNames.add(aclObj.getAccessorXPermitNames(i));
				prop.addValue(new DfValue(aclObj.getAccessorXPermitNames(i)));
			}
		}
	}

	/**
	 * Exports supporting objects of this acl object. It exports accessor names of an acl object.
	 *
	 * @param aclObj
	 *            the ACL object in CMS
	 * @throws CMSMFException
	 *             the cMSMF exception
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private void exportSupportingObjects(IDfACL aclObj) throws CMSMFException, DfException {
		// Export accessor names except dm_world and dm_owner
		final IDfSession session = aclObj.getSession();
		for (int i = 0; i < aclObj.getAccessorCount(); i++) {
			String accessorName = aclObj.getAccessorName(i);
			if (!accessorName.equals(DctmAttrNameConstants.ACCESSOR_NAME_DM_WORLD)
				&& !accessorName.equals(DctmAttrNameConstants.ACCESSOR_NAME_DM_OWNER)
				&& !accessorName.equals(DctmAttrNameConstants.ACCESSOR_NAME_DM_GROUP)) {
				DctmObjectExportHelper.serializeUserOrGroupByName(session, accessorName);
			}
		}
	}

}
