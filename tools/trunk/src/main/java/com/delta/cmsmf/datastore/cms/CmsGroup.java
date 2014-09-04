/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.datastore.cms.CmsAttributeHandlers.AttributeHandler;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class CmsGroup extends CmsObject<IDfGroup> {

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsGroup.HANDLERS_READY) { return; }
		AttributeHandler handler = new AttributeHandler() {

			@Override
			public Collection<IDfValue> getImportableValues(IDfPersistentObject object, DataAttribute attribute)
				throws DfException {
				return null;
			}

			@Override
			public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr)
				throws DfException {
				return null;
			}

			@Override
			public boolean includeInImport(IDfPersistentObject object, DataAttribute attribute) throws DfException {
				return false;
			}

			@Override
			public boolean includeInExport(IDfPersistentObject object, IDfAttr attr) throws DfException {
				return true;
			}
		};
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, DataType.DF_STRING,
			DctmAttrNameConstants.GROUP_NAME, handler);
		CmsGroup.HANDLERS_READY = true;
	}

	public CmsGroup() {
		super(CmsObjectType.GROUP, IDfGroup.class);
		CmsGroup.initHandlers();
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfGroup user) throws DfException {
	}

	@Override
	protected IDfGroup locateInCms(IDfSession session) throws DfException {
		return session.getGroup(getAttribute(DctmAttrNameConstants.GROUP_NAME).getSingleValue().asString());
	}

	/*
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
	 */
}