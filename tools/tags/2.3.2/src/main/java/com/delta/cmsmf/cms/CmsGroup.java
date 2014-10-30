/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;

import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.CmsAttributeHandlers.AttributeHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsGroup extends CmsObject<IDfGroup> {

	private static final String USERS_WITH_DEFAULT_GROUP = "usersWithDefaultGroup";

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsGroup.HANDLERS_READY) { return; }
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING, CmsAttributes.GROUP_ADMIN,
			CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING, CmsAttributes.OWNER_NAME,
			CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING, CmsAttributes.GROUP_NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING,
			CmsAttributes.GROUPS_NAMES, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.GROUP, CmsDataType.DF_STRING, CmsAttributes.USERS_NAMES,
			new AttributeHandler() {
			@Override
			public boolean includeInImport(IDfPersistentObject object, CmsAttribute attribute) throws DfException {
				return false;
			}

			@Override
			public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr)
				throws DfException {
				return CmsMappingUtils.substituteMappableUsers(object, attr);
			}

		});
		CmsGroup.HANDLERS_READY = true;
	}

	private static boolean SPECIAL_GROUPS_READY = false;
	private static Set<String> SPECIAL_GROUPS = Collections.emptySet();

	private static synchronized void initSpecialGroups() {
		if (CmsGroup.SPECIAL_GROUPS_READY) { return; }
		String specialGroups = Setting.SPECIAL_GROUPS.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(specialGroups);
		CmsGroup.SPECIAL_GROUPS = Collections.unmodifiableSet(new HashSet<String>(strTokenizer.getTokenList()));
		CmsGroup.SPECIAL_GROUPS_READY = true;
	}

	public static boolean isSpecialGroup(String group) {
		CmsGroup.initSpecialGroups();
		return CmsGroup.SPECIAL_GROUPS.contains(group);
	}

	/**
	 * This DQL will find all users for which this group is marked as the default group, and thus
	 * all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_GROUP = "SELECT u.user_name FROM dm_user u, dm_group g WHERE u.user_group_name = g.group_name AND g.r_object_id = '%s'";

	public CmsGroup() {
		super(IDfGroup.class);
		CmsGroup.initHandlers();
		CmsGroup.initSpecialGroups();
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
	protected void getDataProperties(Collection<CmsProperty> properties, IDfGroup group) throws DfException {
		// Store all the users that have this group as their default group
		CmsProperty property = new CmsProperty(CmsGroup.USERS_WITH_DEFAULT_GROUP, CmsDataType.DF_STRING);
		for (IDfValue v : getUsersWithDefaultGroup(group)) {
			property.addValue(v);
		}
	}

	@Override
	protected void doPersistDependents(IDfGroup group, CmsTransferContext ctx, CmsDependencyManager dependencyManager)
		throws DfException, CMSMFException {
		final IDfSession session = group.getSession();
		String groupOwner = group.getOwnerName();
		if (!CmsMappingUtils.isSubstitutedUser(session, groupOwner) && !CmsUser.isSpecialUser(groupOwner)) {
			IDfUser owner = session.getUser(groupOwner);
			if (owner != null) {
				dependencyManager.persistRelatedObject(owner);
			} else {
				throw new CMSMFException(String.format(
					"Missing dependency for group [%s] - user [%s] not found (as group owner)", group.getGroupName(),
					groupOwner));
			}
		} else {
			this.log.warn(String.format("Skipping export of special user [%s] as the owner of group [%s]", groupOwner,
				group.getGroupName()));
		}

		String groupAdmin = group.getGroupAdmin();
		if (!CmsMappingUtils.isSubstitutedUser(session, groupAdmin) && !CmsUser.isSpecialUser(groupAdmin)) {
			IDfUser admin = session.getUser(groupAdmin);
			if (admin != null) {
				dependencyManager.persistRelatedObject(admin);
			} else {
				throw new CMSMFException(String.format(
					"Missing dependency for group [%s] - user [%s] not found (as group admin)", group.getGroupName(),
					groupAdmin));
			}
		} else {
			this.log.warn(String.format("Skipping export of special user [%s] as the admin of group [%s]", groupAdmin,
				group.getGroupName()));
		}

		// Avoid calling DQL twice
		CmsProperty property = getProperty(CmsGroup.USERS_WITH_DEFAULT_GROUP);
		Iterable<IDfValue> it = property;
		if (property == null) {
			// IF the property hasn't been set, we do the DQL...
			it = getUsersWithDefaultGroup(group);
		}
		for (IDfValue v : it) {
			if (CmsMappingUtils.isSubstitutedUser(session, v.asString())) {
				// This is a special user, we don't add it as a dependency
				continue;
			}
			IDfUser user = session.getUser(v.asString());
			if (user == null) {
				// in theory, this should be impossible as we just got the list via a direct query
				// to dm_user, and thus the users listed do exist
				throw new CMSMFException(String.format(
					"WARNING: Missing dependency for group [%s] - user [%s] not found (as default group)",
					group.getGroupName(), v.asString()));
			}
			dependencyManager.persistRelatedObject(user);
		}

		CmsAttribute usersNames = getAttribute(CmsAttributes.USERS_NAMES);
		if (usersNames != null) {
			for (IDfValue v : usersNames) {
				String userName = v.asString();
				if (CmsUser.isSpecialUser(userName)) {
					this.log.warn(String.format("Will not persist special member user dependency [%s] for group [%s]",
						userName, group.getGroupName()));
					continue;
				}
				// We can check against whether the user is ${...} or a normal one because
				// we will have already performed the mappings, hence why we use getAttribute()
				// instead of getting the attribute direct from the object
				if (CmsMappingUtils.isSubstitutionForMappableUser(userName)) {
					// User is mapped to a special user, so we shouldn't include it as a dependency
					// because it will be mapped on the target
					continue;
				}

				IDfUser member = session.getUser(userName);
				if (member == null) {
					String msg = String.format(
						"Missing dependency for group [%s] - user [%s] not found (as group member)",
						group.getGroupName(), userName);
					this.log.warn(msg);
					ctx.printf(msg);
					continue;
				}
				dependencyManager.persistRelatedObject(member);
			}
		}

		CmsAttribute groupsNames = getAttribute(CmsAttributes.GROUPS_NAMES);
		if (groupsNames != null) {
			for (IDfValue v : groupsNames) {
				String groupName = v.asString();
				if (CmsGroup.isSpecialGroup(groupName)) {
					this.log.warn(String.format("Will not persist special member group dependency [%s] for group [%s]",
						groupName, group.getGroupName()));
					continue;
				}

				IDfGroup member = session.getGroup(groupName);
				if (member == null) {
					String msg = String.format(
						"Missing dependency for group [%s] - group [%s] not found (as group member)",
						group.getGroupName(), groupName);
					this.log.warn(msg);
					ctx.printf(msg);
					continue;
				}
				dependencyManager.persistRelatedObject(member);
			}
		}
	}

	@Override
	protected void finalizeConstruction(IDfGroup group, boolean newObject, CmsTransferContext context)
		throws DfException {
		final IDfValue groupName = getAttribute(CmsAttributes.GROUP_NAME).getValue();
		if (newObject) {
			copyAttributeToObject(CmsAttributes.GROUP_NAME, group);
		}
		final IDfSession session = group.getSession();
		CmsAttribute usersNames = getAttribute(CmsAttributes.USERS_NAMES);
		// Keep track of missing users so we don't look for them again.
		Set<String> missingUsers = new HashSet<String>();
		if (usersNames != null) {
			group.removeAllUsers();
			for (IDfValue v : usersNames) {
				final String actualUser = CmsMappingUtils.resolveMappableUser(session, v.asString());
				final IDfUser user = session.getUser(actualUser);
				if (user == null) {
					missingUsers.add(actualUser);
					this.log
					.warn(String
						.format(
							"Failed to add user [%s] as a member of [%s] - the user wasn't found - probably didn't need to be copied over",
							actualUser, groupName.asString()));
					continue;
				}
				group.addUser(actualUser);
			}
		}

		CmsAttribute groupsNames = getAttribute(CmsAttributes.GROUPS_NAMES);
		if (groupsNames != null) {
			group.removeAllGroups();
			for (IDfValue v : groupsNames) {
				final String actualGroup = v.asString();
				final IDfGroup other = session.getGroup(actualGroup);
				if (other == null) {
					this.log
					.warn(String
						.format(
							"Failed to add group [%s] as a member of [%s] - the group wasn't found - probably didn't need to be copied over",
							actualGroup, groupName.asString()));
					continue;
				}
				group.addGroup(actualGroup);
			}
		}

		// Set this group as users' default group
		CmsProperty property = getProperty(CmsGroup.USERS_WITH_DEFAULT_GROUP);
		if (property != null) {
			for (IDfValue v : property) {
				final String actualUser = CmsMappingUtils.resolveMappableUser(session, v.asString());
				if (missingUsers.contains(actualUser)) {
					continue;
				}
				final IDfUser user = session.getUser(actualUser);
				if (user == null) {
					this.log
					.warn(String
						.format(
							"Failed to set group [%s] as the default group for the user [%s] - the user wasn't found - probably didn't need to be copied over",
							groupName.asString(), actualUser));
					continue;
				}
				user.setUserGroupName(groupName.asString());
				user.save();
				// Update the system attributes, if we can
				try {
					updateSystemAttributes(user, context);
				} catch (CMSMFException e) {
					this.log
					.warn(
						String
						.format(
							"Failed to update the system attributes for user [%s] after assigning group [%s] as their default group",
							actualUser, group.getGroupName()), e);
				}
			}
		}
	}

	@Override
	protected boolean isValidForLoad(IDfGroup group) throws DfException {
		if (CmsGroup.isSpecialGroup(group.getGroupName())) { return false; }
		return super.isValidForLoad(group);
	}

	@Override
	protected boolean skipImport(CmsTransferContext ctx) throws DfException {
		IDfValue groupNameValue = getAttribute(CmsAttributes.GROUP_NAME).getValue();
		final String groupName = groupNameValue.asString();
		if (CmsGroup.isSpecialGroup(groupName)) { return true; }
		return super.skipImport(ctx);
	}

	@Override
	protected IDfGroup locateInCms(CmsTransferContext ctx) throws DfException {
		return ctx.getSession().getGroup(getAttribute(CmsAttributes.GROUP_NAME).getValue().asString());
	}
}