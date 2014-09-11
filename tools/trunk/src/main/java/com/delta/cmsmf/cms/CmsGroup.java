/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsGroup extends CmsObject<IDfGroup> {

	private static final String USERS_WITH_DEFAULT_GROUP = "usersWithDefaultGroup";

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsGroup.HANDLERS_READY) { return; }
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING, CmsAttributes.GROUP_NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING,
			CmsAttributes.GROUPS_NAMES, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING, CmsAttributes.USERS_NAMES,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsGroup.HANDLERS_READY = true;
	}

	/**
	 * This DQL will find all users for which this group is marked as the default group,
	 * and thus all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_GROUP = "SELECT u.user_name FROM dm_user u, dm_group g WHERE u.user_group_name = g.group_name AND g.r_object_id = '%s'";

	public CmsGroup() {
		super(CmsObjectType.GROUP, IDfGroup.class);
		CmsGroup.initHandlers();
	}

	@Override
	protected String calculateLabel(IDfGroup group) throws DfException {
		return group.getGroupName();
	}

	private Collection<IDfValue> getUsersWithDefaultGroup(IDfGroup group) throws DfException {
		IDfCollection resultCol = DfUtils.executeQuery(group.getSession(),
			String.format(CmsGroup.DQL_FIND_USERS_WITH_DEFAULT_GROUP, group.getObjectId().getId()),
			IDfQuery.DF_EXECREAD_QUERY);
		try {
			Collection<IDfValue> ret = new ArrayList<IDfValue>();
			while (resultCol.next()) {
				ret.add(resultCol.getValueAt(0));
			}
			return ret;
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	@Override
	protected boolean isValidForLoad(IDfGroup group) throws DfException {
		return !group.getGroupName().startsWith("dm_");
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfGroup group) throws DfException {
		// Store all the users that have this group as their default group
		CmsProperty property = new CmsProperty(CmsGroup.USERS_WITH_DEFAULT_GROUP, CmsDataType.DF_STRING);
		for (IDfValue v : getUsersWithDefaultGroup(group)) {
			property.addValue(v);
		}
	}

	@Override
	protected void doPersistDependencies(IDfGroup group, CmsDependencyManager dependencyManager) throws DfException,
	CMSMFException {
		final IDfSession session = group.getSession();
		IDfUser owner = session.getUser(group.getOwnerName());
		if (owner != null) {
			dependencyManager.persistDependency(owner);
		} else {
			this.log.warn(String.format(
				"WARNING: Missing dependency for group [%s] - user [%s] not found (as group owner)",
				group.getGroupName(), group.getOwnerName()));
		}

		IDfUser admin = session.getUser(group.getGroupAdmin());
		if (admin != null) {
			dependencyManager.persistDependency(admin);
		} else {
			this.log.warn(String.format(
				"WARNING: Missing dependency for group [%s] - user [%s] not found (as group admin)",
				group.getGroupName(), group.getGroupAdmin()));
		}

		// Avoid calling DQL twice
		CmsProperty property = getProperty(CmsGroup.USERS_WITH_DEFAULT_GROUP);
		Iterable<IDfValue> it = property;
		if (property == null) {
			// IF the property hasn't been set, we do the DQL...
			it = getUsersWithDefaultGroup(group);
		}
		for (IDfValue v : it) {
			IDfUser user = session.getUser(v.asString());
			if (user == null) {
				this.log.warn(String.format(
					"WARNING: Missing dependency for group [%s] - user [%s] not found (as default group)",
					group.getGroupName(), v.asString()));
				continue;
			}
			dependencyManager.persistDependency(user);
		}

		CmsAttribute usersNames = getAttribute(CmsAttributes.USERS_NAMES);
		if (usersNames != null) {
			for (IDfValue v : usersNames) {
				IDfUser member = session.getUser(v.asString());
				if (member == null) {
					this.log.warn(String.format(
						"WARNING: Missing dependency for group [%s] - user [%s] not found (as group member)",
						group.getGroupName(), v.asString()));
					continue;
				}
				dependencyManager.persistDependency(member);
			}
		}

		CmsAttribute groupsNames = getAttribute(CmsAttributes.GROUPS_NAMES);
		if (groupsNames != null) {
			for (IDfValue v : groupsNames) {
				IDfGroup member = session.getGroup(v.asString());
				if (member == null) {
					this.log.warn(String.format(
						"WARNING: Missing dependency for group [%s] - group [%s] not found (as group member)",
						group.getGroupName(), v.asString()));
					continue;
				}
				dependencyManager.persistDependency(member);
			}
		}
	}

	@Override
	protected IDfGroup newObject(IDfSession session) throws DfException {
		return super.newObject(session);
	}

	@Override
	protected void finalizeConstruction(IDfGroup object, boolean newObject) throws DfException {
		IDfValue groupName = getAttribute(CmsAttributes.GROUP_NAME).getValue();
		if (newObject) {
			copyAttributeToObject(CmsAttributes.GROUP_NAME, object);
		}
		final IDfSession session = object.getSession();
		CmsAttribute usersNames = getAttribute(CmsAttributes.USERS_NAMES);
		// Keep track of missing users so we don't look for them again.
		Set<String> missingUsers = new HashSet<String>();
		if (usersNames != null) {
			// TODO: Support merging in the future?
			List<IDfValue> actualUsers = new ArrayList<IDfValue>();
			for (IDfValue v : usersNames) {
				IDfUser user = session.getUser(v.asString());
				if (user == null) {
					missingUsers.add(v.asString());
					this.log
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

		// Set this group as users' default group
		CmsProperty property = getProperty(CmsGroup.USERS_WITH_DEFAULT_GROUP);
		if (property != null) {
			for (IDfValue v : property) {
				if (missingUsers.contains(v.asString())) {
					continue;
				}
				IDfUser user = session.getUser(v.asString());
				if (user == null) {
					this.log
						.warn(String
							.format(
								"Failed to link Group [%s] to user [%s] as the user's default group - the user wasn't found - probably didn't need to be copied over",
								groupName.asString(), v.asString()));
					continue;
				}
				user.setUserGroupName(groupName.asString());
			}
		}
	}

	@Override
	public void resolveDependencies(IDfGroup group, CmsAttributeMapper mapper) throws DfException, CMSMFException {
		final String groupName = group.getGroupName();
		final IDfSession session = group.getSession();
		CmsAttribute groupsNames = getAttribute(CmsAttributes.GROUPS_NAMES);
		if (groupsNames != null) {
			// TODO: Support merging in the future?
			List<IDfValue> actualGroups = new ArrayList<IDfValue>();
			for (IDfValue v : groupsNames) {
				IDfGroup other = session.getGroup(v.asString());
				if (other == null) {
					this.log
					.warn(String
						.format(
							"Failed to link Group [%s] to group [%s] as a member - the group wasn't found - probably didn't need to be copied over",
							groupName, v.asString()));
					continue;
				}
				actualGroups.add(v);
			}
			setAttributeOnObject(groupsNames, actualGroups, group);
		}
	}

	@Override
	protected IDfGroup locateInCms(IDfSession session) throws DfException {
		return session.getGroup(getAttribute(CmsAttributes.GROUP_NAME).getValue().asString());
	}
}