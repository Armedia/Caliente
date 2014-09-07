/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.cms.CmsAttributeHandlers.AttributeHandler;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
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
			public boolean includeInImport(IDfPersistentObject object, CmsAttribute attribute) throws DfException {
				return false;
			}
		};
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING,
			DctmAttrNameConstants.GROUP_NAME, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING,
			DctmAttrNameConstants.GROUPS_NAMES, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING,
			DctmAttrNameConstants.USERS_NAMES, handler);
		CmsGroup.HANDLERS_READY = true;
	}

	public CmsGroup() {
		super(CmsObjectType.GROUP, IDfGroup.class);
		CmsGroup.initHandlers();
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfGroup user) throws DfException {
	}

	@Override
	protected IDfGroup newObject(IDfSession session) throws DfException {
		return super.newObject(session);
	}

	@Override
	protected void finalizeConstruction(IDfGroup object, boolean newObject) throws DfException {
		IDfValue groupName = getAttribute(DctmAttrNameConstants.GROUP_NAME).getValue();
		if (newObject) {
			copyAttributeToObject(DctmAttrNameConstants.GROUP_NAME, object);
		}
		// TODO: Support merging in the future?
		// TODO: Support a two-pass approach per-tier, where required? This will help with
		// horizontal dependencies like group-on-group action
		IDfSession session = object.getSession();
		CmsAttribute groupsNames = getAttribute(DctmAttrNameConstants.GROUPS_NAMES);
		if (groupsNames != null) {
			List<IDfValue> actualGroups = new ArrayList<IDfValue>();
			for (IDfValue v : groupsNames) {
				IDfGroup group = session.getGroup(v.asString());
				if (group == null) {
					this.logger
					.warn(String
						.format(
							"Failed to link Group [%s] to group [%s] as a member - the group wasn't found - probably didn't need to be copied over",
							groupName.asString(), v.asString()));
					continue;
				}
				actualGroups.add(v);
			}
			setAttributeOnObject(groupsNames, actualGroups, object);
		}
		CmsAttribute usersNames = getAttribute(DctmAttrNameConstants.USERS_NAMES);
		if (usersNames != null) {
			List<IDfValue> actualUsers = new ArrayList<IDfValue>();
			for (IDfValue v : usersNames) {
				IDfUser user = session.getUser(v.asString());
				if (user == null) {
					this.logger
						.warn(String
							.format(
								"Failed to link Group [%s] to user [%s] as a member - the user wasn't found - probably didn't need to be copied over",
								groupName.asString(), v.asString()));
					continue;
				}
				actualUsers.add(v);
			}
			setAttributeOnObject(usersNames, actualUsers, object);
		}
	}

	@Override
	protected IDfGroup locateInCms(IDfSession session) throws DfException {
		return session.getGroup(getAttribute(DctmAttrNameConstants.GROUP_NAME).getValue().asString());
	}
}