/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.common.DctmGroup;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.commons.dfc.util.DfUtils;
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
	protected boolean isSameObject(IDfGroup existingObject, DctmImportContext ctx) throws DfException, ImportException {
		if (!super.isSameObject(existingObject, ctx)) { return false; }

		// Same name?
		if (!isAttributeEquals(existingObject, DctmAttributes.GROUP_NAME)) { return false; }

		Set<String> existingMembers = new HashSet<>();

		// Same group-members?
		int c = existingObject.getValueCount(DctmAttributes.GROUPS_NAMES);
		for (int i = 0; i < c; i++) {
			existingMembers.add(existingObject.getRepeatingString(DctmAttributes.GROUPS_NAMES, i).toLowerCase());
		}

		CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.GROUPS_NAMES);
		if (att == null) {
			if (existingMembers.isEmpty()) { return false; }
		} else {
			for (IDfValue v : att) {
				if (!existingMembers.remove(v.asString().toLowerCase())) { return false; }
			}
			if (existingMembers.isEmpty()) { return false; }
		}

		if (ctx.isSupported(CmfObject.Archetype.USER)) {
			// Same user-members?
			existingMembers = new HashSet<>();
			c = existingObject.getValueCount(DctmAttributes.USERS_NAMES);
			for (int i = 0; i < c; i++) {
				existingMembers.add(existingObject.getRepeatingString(DctmAttributes.USERS_NAMES, i));
			}
			att = this.cmfObject.getAttribute(DctmAttributes.USERS_NAMES);
			if (att == null) {
				if (existingMembers.isEmpty()) { return false; }
			} else {
				for (IDfValue v : att) {
					final String name = DctmMappingUtils.resolveMappableUser(ctx.getSession(), v.asString());
					if (!existingMembers.remove(name)) { return false; }
				}
				if (existingMembers.isEmpty()) { return false; }
			}
		}

		// TODO: Check other things?
		return true;
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
		final IDfSession session = context.getSession();
		CmfAttribute<IDfValue> usersNames = this.cmfObject.getAttribute(DctmAttributes.USERS_NAMES);
		// Keep track of missing users so we don't look for them again.
		Set<String> missingUsers = new HashSet<>();
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
					if (context.isSupported(CmfObject.Archetype.USER)) { throw new ImportException(msg); }
					this.log.warn(msg);
					continue;
				}
				if (user == null) {
					missingUsers.add(actualUser);
					String msg = String.format(
						"Failed to add user [%s] as a member of [%s] - the user wasn't found - probably didn't need to be copied over",
						actualUser, groupName.asString());
					if (context.isSupported(CmfObject.Archetype.USER)) { throw new ImportException(msg); }
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
						this.log.warn("Failed to set user [{}] as the owner for group [{}] - the user wasn't found",
							actualUser, groupName.asString());
					}
				} catch (ImportException e) {
					this.log.warn("Failed to set user [{}] as the owner for group [{}] - {}", actualUser,
						groupName.asString(), e.getMessage());
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
						this.log.warn(
							"Failed to set user [{}] as the administrator for group [{}] - the user wasn't found",
							actualUser, groupName.asString());
					}
				} catch (ImportException e) {
					this.log.warn("Failed to set user [{}] as the administrator for group [{}] - {}", actualUser,
						groupName.asString(), e.getMessage());
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
					try {
						group.addGroup(actualGroup);
					} catch (DfException e) {
						this.log.warn(
							"Failed to add group [{}] as a member of [{}] - the group wasn't found and couldn't be added by name",
							actualGroup, groupName.asString(), e);
					}
				} else {
					group.addGroup(other.getGroupName());
				}
			}
		}
	}

	@Override
	protected void updateReferenced(IDfGroup group, DctmImportContext context) throws DfException, ImportException {
		final IDfSession session = context.getSession();
		final String groupName = group.getGroupName();

		// Set this group as users' default group
		CmfProperty<IDfValue> property = this.cmfObject.getProperty(IntermediateProperty.USERS_WITH_DEFAULT_GROUP);
		if ((property == null) || (property.getValueCount() == 0)) { return; }
		Set<String> users = new TreeSet<>();
		for (IDfValue v : property) {
			String user = v.asString();
			// Don't touch the special users!
			if (context.isUntouchableUser(user)) {
				this.log.warn("Will not substitute the default group for the special user [{}]",
					DctmMappingUtils.resolveMappableUser(session, user));
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
				if (context.isSupported(CmfObject.Archetype.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
				continue;
			}
			if (user == null) {
				String msg = String.format(
					"Failed to set group [%s] as the default group for the user [%s] - the user wasn't found - probably didn't need to be copied over",
					groupName, actualUser);
				if (context.isSupported(CmfObject.Archetype.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
				continue;
			}
			if (Tools.equals(groupName, user.getUserGroupName())) {
				continue;
			}
			this.log.info("Setting group [{}] as the default group for user [{}]", groupName, user.getUserName());
			DfUtils.lockObject(this.log, user);
			user.fetch(null);
			user.setUserGroupName(groupName);
			user.save();
			// Update the system attributes, if we can
			try {
				updateSystemAttributes(user, context);
			} catch (ImportException e) {
				this.log.warn(
					"Failed to update the system attributes for user [{}] after assigning group [{}] as their default group",
					actualUser, group.getGroupName(), e);
			}
		}
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws DfException, ImportException {
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