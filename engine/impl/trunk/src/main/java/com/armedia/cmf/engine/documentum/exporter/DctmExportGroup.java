/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collection;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmGroup;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfType;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportGroup extends DctmExportDelegate<IDfGroup> implements DctmGroup {

	/**
	 * This DQL will find all users for which this group is marked as the default group, and thus
	 * all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_GROUP = "SELECT u.user_name FROM dm_user u, dm_group g WHERE u.user_group_name = g.group_name AND g.r_object_id = '%s'";

	protected DctmExportGroup(DctmExportDelegateFactory factory, IDfGroup group) throws Exception {
		super(factory, IDfGroup.class, group);
	}

	DctmExportGroup(DctmExportDelegateFactory factory, IDfPersistentObject group) throws Exception {
		this(factory, DctmExportDelegate.staticCast(IDfGroup.class, group));
	}

	@Override
	protected String calculateBatchId(IDfGroup object) throws Exception {
		return "NO_BATCH";
	}

	@Override
	protected String calculateLabel(IDfGroup group) throws Exception {
		return group.getGroupName();
	}

	@Override
	protected void getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties, IDfGroup group)
		throws DfException {
		// CmfStore all the users that have this group as their default group
		CmfProperty<IDfValue> property = new CmfProperty<IDfValue>(DctmGroup.USERS_WITH_DEFAULT_GROUP,
			DctmDataType.DF_STRING.getStoredType());
		IDfCollection resultCol = DfUtils.executeQuery(group.getSession(),
			String.format(DctmExportGroup.DQL_FIND_USERS_WITH_DEFAULT_GROUP, group.getObjectId().getId()),
			IDfQuery.DF_EXECREAD_QUERY);
		try {
			while (resultCol.next()) {
				IDfValue v = resultCol.getValueAt(0);
				String mapped = DctmMappingUtils.substituteMappableUsers(ctx.getSession(), v.asString());
				if (DctmMappingUtils.isSubstitutionForMappableUser(mapped) || ctx.isSpecialUser(v.asString())) {
					// Special users don't get their default groups modified
					continue;
				}
				property.addValue(DfValueFactory.newStringValue(mapped));
			}
			properties.add(property);
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfGroup group, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> ret = super.findRequirements(session, marshaled, group, ctx);

		String groupOwner = group.getOwnerName();
		if (!DctmMappingUtils.isMappableUser(session, groupOwner) && !ctx.isSpecialUser(groupOwner)) {
			IDfUser owner = session.getUser(groupOwner);
			if (owner == null) { throw new Exception(String.format(
				"Missing dependency for group [%s] - user [%s] not found (as group owner)", group.getGroupName(),
				groupOwner)); }
			ret.add(this.factory.newExportDelegate(owner));
		} else {
			this.log.warn(String.format("Skipping export of special user [%s] as the owner of group [%s]", groupOwner,
				group.getGroupName()));
		}

		String groupAdmin = group.getGroupAdmin();
		if (!DctmMappingUtils.isMappableUser(session, groupAdmin) && !ctx.isSpecialUser(groupAdmin)) {
			IDfUser admin = session.getUser(groupAdmin);
			if (admin == null) { throw new Exception(String.format(
				"Missing dependency for group [%s] - user [%s] not found (as group admin)", group.getGroupName(),
				groupAdmin)); }
			ret.add(this.factory.newExportDelegate(admin));
		} else {
			this.log.warn(String.format("Skipping export of special user [%s] as the admin of group [%s]", groupAdmin,
				group.getGroupName()));
		}

		CmfAttribute<IDfValue> usersNames = marshaled.getAttribute(DctmAttributes.USERS_NAMES);
		if (usersNames != null) {
			for (IDfValue v : usersNames) {
				String userName = v.asString();
				if (ctx.isSpecialUser(userName)) {
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
					if (ctx.isSupported(CmfType.USER)) { throw new Exception(msg); }
					this.log.warn(msg);
					ctx.printf(msg);
				}
				ret.add(this.factory.newExportDelegate(member));
			}
		}

		CmfAttribute<IDfValue> groupsNames = marshaled.getAttribute(DctmAttributes.GROUPS_NAMES);
		if (groupsNames != null) {
			for (IDfValue v : groupsNames) {
				String groupName = v.asString();
				if (ctx.isSpecialGroup(groupName) || DctmMappingUtils.SPECIAL_NAMES.contains(groupName)) {
					this.log.warn(String.format("Will not persist special member group dependency [%s] for group [%s]",
						groupName, group.getGroupName()));
					continue;
				}

				IDfGroup member = session.getGroup(groupName);
				if (member != null) {
					ret.add(this.factory.newExportDelegate(member));
					continue;
				}
				if (!DctmMappingUtils.SPECIAL_NAMES.contains(groupName)) {
					// Make sure to explode because this is a group that's expected to exist
					throw new Exception(String.format(
						"Missing dependency for group [%s] - group [%s] not found (as group member)",
						group.getGroupName(), groupName));
				}
			}
		}

		IDfId groupDir = group.getGroupDirectoryId();
		if (groupDir.isObjectId() && !groupDir.isNull()) {
			try {
				ret.add(this.factory.newExportDelegate(session.getObject(groupDir)));
			} catch (DfIdNotFoundException e) {
				// Folder not found...warn, but don't fail
				this.log.warn(String.format("Group [%s] references a directory with ID[%s], but it couldn't be found",
					group.getGroupName(), groupDir.getId()));
			}
		}
		return ret;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findDependents(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfGroup group, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> ret = super.findDependents(session, marshaled, group, ctx);

		// Avoid calling DQL twice
		CmfProperty<IDfValue> property = marshaled.getProperty(DctmGroup.USERS_WITH_DEFAULT_GROUP);
		if (property == null) { throw new Exception(String.format(
			"The export for group [%s] does not contain the critical property [%s]", marshaled.getLabel(),
			DctmGroup.USERS_WITH_DEFAULT_GROUP)); }

		for (IDfValue v : property) {
			IDfUser user = session.getUser(v.asString());
			if (user == null) {
				// in theory, this should be impossible as we just got the list via a direct query
				// to dm_user, and thus the users listed do exist
				throw new Exception(String.format(
					"Missing dependent for group [%s] - user [%s] not found (as default group)", group.getGroupName(),
					v.asString()));
			}
			ret.add(this.factory.newExportDelegate(user));
		}
		return ret;
	}
}