/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import java.util.HashSet;
import java.util.Set;

import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmMappingUtils;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.common.DctmGroup;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
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

	public DctmImportGroup(DctmImportEngine engine, StoredObject<IDfValue> storedObject) {
		super(engine, DctmObjectType.GROUP, storedObject);
	}

	@Override
	protected String calculateLabel(IDfGroup group) throws DfException {
		return group.getGroupName();
	}

	@Override
	protected void finalizeConstruction(IDfGroup group, boolean newObject, DctmImportContext context)
		throws DfException {
		final IDfValue groupName = this.storedObject.getAttribute(DctmAttributes.GROUP_NAME).getValue();
		if (newObject) {
			copyAttributeToObject(DctmAttributes.GROUP_NAME, group);
		}
		final IDfSession session = group.getSession();
		StoredAttribute<IDfValue> usersNames = this.storedObject.getAttribute(DctmAttributes.USERS_NAMES);
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

		StoredAttribute<IDfValue> groupsNames = this.storedObject.getAttribute(DctmAttributes.GROUPS_NAMES);
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
		StoredProperty<IDfValue> property = this.storedObject.getProperty(DctmGroup.USERS_WITH_DEFAULT_GROUP);
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
				user.lock();
				user.fetch(null);
				user.setUserGroupName(groupName.asString());
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
	}

	@Override
	protected boolean isValidForLoad(DctmImportContext ctx, IDfGroup group) throws DfException {
		if (ctx.isSpecialGroup(group.getGroupName())) { return false; }
		return super.isValidForLoad(ctx, group);
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws DfException {
		IDfValue groupNameValue = this.storedObject.getAttribute(DctmAttributes.GROUP_NAME).getValue();
		final String groupName = groupNameValue.asString();
		if (ctx.isSpecialGroup(groupName)) { return true; }
		return super.skipImport(ctx);
	}

	@Override
	protected IDfGroup locateInCms(DctmImportContext ctx) throws DfException {
		return ctx.getSession().getGroup(
			this.storedObject.getAttribute(DctmAttributes.GROUP_NAME).getValue().asString());
	}
}