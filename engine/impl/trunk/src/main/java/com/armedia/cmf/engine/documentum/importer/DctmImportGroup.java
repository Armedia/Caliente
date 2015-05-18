/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.common.DctmGroup;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportGroup extends DctmImportDelegate<IDfGroup> implements DctmGroup {

	public DctmImportGroup(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfGroup.class, DctmObjectType.GROUP, storedObject);
	}

	@Override
	protected String calculateLabel(IDfGroup group) throws DfException {
		return group.getGroupName();
	}

	@Override
	protected void finalizeConstruction(IDfGroup group, boolean newObject, DctmImportContext context)
		throws ImportException, DfException {
		final IDfValue groupName = this.cmfObject.getAttribute(DctmAttributes.GROUP_NAME).getValue();
		if (newObject) {
			group.setGroupName(groupName.asString().toLowerCase());
		}
		final IDfSession session = group.getSession();
		CmfAttribute<IDfValue> usersNames = this.cmfObject.getAttribute(DctmAttributes.USERS_NAMES);
		// Keep track of missing users so we don't look for them again.
		Set<String> missingUsers = new HashSet<String>();
		if (usersNames != null) {
			group.removeAllUsers();
			for (IDfValue v : usersNames) {
				final String actualUser = DctmMappingUtils.resolveMappableUser(session, v.asString());
				final IDfUser user;
				try {
					user = DctmImportUser.locateExistingUser(context, actualUser);
				} catch (MultipleUserMatchesException e) {
					String msg = String.format("Failed to add user [%s] as a member of [%s] - %s", actualUser,
						groupName.asString(), e.getMessage());
					if (context.isSupported(CmfType.USER)) { throw new ImportException(msg); }
					this.log.warn(msg);
					continue;
				}
				if (user == null) {
					missingUsers.add(actualUser);
					String msg = String
						.format(
							"Failed to add user [%s] as a member of [%s] - the user wasn't found - probably didn't need to be copied over",
							actualUser, groupName.asString());
					if (context.isSupported(CmfType.USER)) { throw new ImportException(msg); }
					this.log.warn(msg);
					continue;
				}
				group.addUser(user.getUserName());
			}
		}

		CmfAttribute<IDfValue> specialUser = this.cmfObject.getAttribute(DctmAttributes.OWNER_NAME);
		if (specialUser != null) {
			final String actualUser = DctmMappingUtils.resolveMappableUser(session, specialUser.getValue().asString());
			if (!StringUtils.isBlank(actualUser)) {
				final IDfUser user;
				try {
					user = DctmImportUser.locateExistingUser(context, actualUser);
					if (user != null) {
						group.setOwnerName(user.getUserName());
					} else {
						this.log.warn(String.format(
							"Failed to set user [%s] as the owner for group [%s] - the user wasn't found", actualUser,
							groupName.asString()));
					}
				} catch (ImportException e) {
					this.log.warn(String.format("Failed to set user [%s] as the owner for group [%s] - %s", actualUser,
						groupName.asString(), e.getMessage()));
				}
			}
		}

		specialUser = this.cmfObject.getAttribute(DctmAttributes.GROUP_ADMIN);
		if (specialUser != null) {
			final String actualUser = DctmMappingUtils.resolveMappableUser(session, specialUser.getValue().asString());
			if (!StringUtils.isBlank(actualUser)) {
				final IDfUser user;
				try {
					user = DctmImportUser.locateExistingUser(context, actualUser);
					if (user != null) {
						group.setGroupAdmin(user.getUserName());
					} else {
						this.log.warn(String.format(
							"Failed to set user [%s] as the administrator for group [%s] - the user wasn't found",
							actualUser, groupName.asString()));
					}
				} catch (ImportException e) {
					this.log.warn(String.format("Failed to set user [%s] as the administrator for group [%s] - %s",
						actualUser, groupName.asString(), e.getMessage()));
				}
			}
		}

		CmfAttribute<IDfValue> groupsNames = this.cmfObject.getAttribute(DctmAttributes.GROUPS_NAMES);
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
	}

	@Override
	protected void updateReferenced(IDfGroup group, DctmImportContext context) throws DfException, ImportException {
		final IDfSession session = context.getSession();
		final String groupName = group.getGroupName();

		// Set this group as users' default group
		CmfProperty<IDfValue> property = this.cmfObject.getProperty(DctmGroup.USERS_WITH_DEFAULT_GROUP);
		if ((property == null) || (property.getValueCount() == 0)) { return; }
		Set<String> users = new TreeSet<String>();
		for (IDfValue v : property) {
			String user = v.asString();
			// Don't touch the special users!
			if (context.isUntouchableUser(user)) {
				this.log.warn(String.format("Will not substitute the default group for the special user [%s]",
					DctmMappingUtils.resolveMappableUser(session, user)));
				continue;
			}
			users.add(user);
		}
		for (String actualUser : users) {
			final IDfUser user;
			try {
				user = DctmImportUser.locateExistingUser(context, actualUser);
			} catch (MultipleUserMatchesException e) {
				String msg = String.format("Failed to set group [%s] as the default group for the user [%s] - %s",
					groupName, actualUser, e.getMessage());
				if (context.isSupported(CmfType.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
				continue;
			}
			if (user == null) {
				String msg = String
					.format(
						"Failed to set group [%s] as the default group for the user [%s] - the user wasn't found - probably didn't need to be copied over",
						groupName, actualUser);
				if (context.isSupported(CmfType.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
				continue;
			}
			if (Tools.equals(groupName, user.getUserGroupName())) {
				continue;
			}
			this.log.info(String.format("Setting group [%s] as the default group for user [%s]", groupName,
				user.getUserName()));
			DfUtils.lockObject(this.log, user);
			user.fetch(null);
			user.setUserGroupName(groupName);
			user.save();
			// Update the system attributes, if we can
			try {
				updateSystemAttributes(user, context);
			} catch (ImportException e) {
				this.log
				.warn(
					String
					.format(
						"Failed to update the system attributes for user [%s] after assigning group [%s] as their default group",
						actualUser, group.getGroupName()), e);
			}
		}
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws DfException {
		IDfValue groupNameValue = this.cmfObject.getAttribute(DctmAttributes.GROUP_NAME).getValue();
		final String groupName = groupNameValue.asString();
		if (ctx.isSpecialGroup(groupName)) { return true; }
		return super.skipImport(ctx);
	}

	@Override
	protected IDfGroup locateInCms(DctmImportContext ctx) throws DfException {
		String groupName = this.cmfObject.getAttribute(DctmAttributes.GROUP_NAME).getValue().asString();
		groupName = groupName.toLowerCase();
		return ctx.getSession().getGroup(groupName);
	}
}