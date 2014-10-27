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

import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredProperty;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.DctmAttributeHandlers.AttributeHandler;
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
public class DctmGroup extends DctmPersistentObject<IDfGroup> {

	private static final String USERS_WITH_DEFAULT_GROUP = "usersWithDefaultGroup";

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (DctmGroup.HANDLERS_READY) { return; }
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.GROUP_ADMIN, DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.OWNER_NAME, DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.GROUP_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.GROUPS_NAMES, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.USERS_NAMES, new AttributeHandler() {
			@Override
			public boolean includeInImport(IDfPersistentObject object, StoredAttribute<IDfValue> attribute)
					throws DfException {
				return false;
			}

			@Override
			public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr)
				throws DfException {
				return DctmMappingUtils.substituteMappableUsers(object, attr);
			}

		});
		DctmGroup.HANDLERS_READY = true;
	}

	private static boolean SPECIAL_GROUPS_READY = false;
	private static Set<String> SPECIAL_GROUPS = Collections.emptySet();

	private static synchronized void initSpecialGroups() {
		if (DctmGroup.SPECIAL_GROUPS_READY) { return; }
		String specialGroups = Setting.SPECIAL_GROUPS.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(specialGroups);
		DctmGroup.SPECIAL_GROUPS = Collections.unmodifiableSet(new HashSet<String>(strTokenizer.getTokenList()));
		DctmGroup.SPECIAL_GROUPS_READY = true;
	}

	public static boolean isSpecialGroup(String group) {
		DctmGroup.initSpecialGroups();
		return DctmGroup.SPECIAL_GROUPS.contains(group);
	}

	/**
	 * This DQL will find all users for which this group is marked as the default group, and thus
	 * all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_GROUP = "SELECT u.user_name FROM dm_user u, dm_group g WHERE u.user_group_name = g.group_name AND g.r_object_id = '%s'";

	public DctmGroup() {
		super(IDfGroup.class);
		DctmGroup.initHandlers();
		DctmGroup.initSpecialGroups();
	}

	@Override
	protected String calculateLabel(IDfGroup group) throws DfException {
		return group.getGroupName();
	}

	private Collection<IDfValue> getUsersWithDefaultGroup(IDfGroup group) throws DfException {
		IDfCollection resultCol = DfUtils.executeQuery(group.getSession(),
			String.format(DctmGroup.DQL_FIND_USERS_WITH_DEFAULT_GROUP, group.getObjectId().getId()),
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
	protected void getDataProperties(Collection<StoredProperty<IDfValue>> properties, IDfGroup group)
		throws DfException {
		// Store all the users that have this group as their default group
		StoredProperty<IDfValue> property = new StoredProperty<IDfValue>(DctmGroup.USERS_WITH_DEFAULT_GROUP,
			DctmDataType.DF_STRING.getStoredType());
		for (IDfValue v : getUsersWithDefaultGroup(group)) {
			property.addValue(v);
		}
	}

	@Override
	protected void doPersistDependents(IDfGroup group, DctmTransferContext ctx, DctmDependencyManager dependencyManager)
		throws DfException, CMSMFException {
		final IDfSession session = group.getSession();
		String groupOwner = group.getOwnerName();
		if (!DctmMappingUtils.isMappableUser(session, groupOwner) && !DctmUser.isSpecialUser(groupOwner)) {
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
		if (!DctmMappingUtils.isMappableUser(session, groupAdmin) && !DctmUser.isSpecialUser(groupAdmin)) {
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
		StoredProperty<IDfValue> property = getProperty(DctmGroup.USERS_WITH_DEFAULT_GROUP);
		Iterable<IDfValue> it = property;
		if (property == null) {
			// IF the property hasn't been set, we do the DQL...
			it = getUsersWithDefaultGroup(group);
		}
		for (IDfValue v : it) {
			if (DctmMappingUtils.isMappableUser(session, v.asString())) {
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

		StoredAttribute<IDfValue> usersNames = getAttribute(DctmAttributes.USERS_NAMES);
		if (usersNames != null) {
			for (IDfValue v : usersNames) {
				String userName = v.asString();
				if (DctmUser.isSpecialUser(userName)) {
					this.log.warn(String.format("Will not persist special member user dependency [%s] for group [%s]",
						userName, group.getGroupName()));
					continue;
				}
				// We can check against whether the user is ${...} or a normal one because
				// we will have already performed the mappings, hence why we use getAttribute()
				// instead of getting the attribute direct from the object
				if (DctmMappingUtils.isSubstitutionForMappableUser(userName)) {
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

		StoredAttribute<IDfValue> groupsNames = getAttribute(DctmAttributes.GROUPS_NAMES);
		if (groupsNames != null) {
			for (IDfValue v : groupsNames) {
				String groupName = v.asString();
				if (DctmGroup.isSpecialGroup(groupName)) {
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
	protected void finalizeConstruction(IDfGroup group, boolean newObject, DctmTransferContext context)
		throws DfException {
		final IDfValue groupName = getAttribute(DctmAttributes.GROUP_NAME).getValue();
		if (newObject) {
			copyAttributeToObject(DctmAttributes.GROUP_NAME, group);
		}
		final IDfSession session = group.getSession();
		StoredAttribute<IDfValue> usersNames = getAttribute(DctmAttributes.USERS_NAMES);
		// Keep track of missing users so we don't look for them again.
		Set<String> missingUsers = new HashSet<String>();
		if (usersNames != null) {
			group.removeAllUsers();
			for (IDfValue v : usersNames) {
				final String actualUser = DctmMappingUtils.resolveMappableUser(session, v.asString());
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

		StoredAttribute<IDfValue> groupsNames = getAttribute(DctmAttributes.GROUPS_NAMES);
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
		StoredProperty<IDfValue> property = getProperty(DctmGroup.USERS_WITH_DEFAULT_GROUP);
		if (property != null) {
			for (IDfValue v : property) {
				final String actualUser = DctmMappingUtils.resolveMappableUser(session, v.asString());
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
		if (DctmGroup.isSpecialGroup(group.getGroupName())) { return false; }
		return super.isValidForLoad(group);
	}

	@Override
	protected boolean skipImport(DctmTransferContext ctx) throws DfException {
		IDfValue groupNameValue = getAttribute(DctmAttributes.GROUP_NAME).getValue();
		final String groupName = groupNameValue.asString();
		if (DctmGroup.isSpecialGroup(groupName)) { return true; }
		return super.skipImport(ctx);
	}

	@Override
	protected IDfGroup locateInCms(DctmTransferContext ctx) throws DfException {
		return ctx.getSession().getGroup(getAttribute(DctmAttributes.GROUP_NAME).getValue().asString());
	}
}