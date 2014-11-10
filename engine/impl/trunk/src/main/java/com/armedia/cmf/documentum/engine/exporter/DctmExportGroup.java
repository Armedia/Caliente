/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.cmf.documentum.engine.DctmAttributeHandlers;
import com.armedia.cmf.documentum.engine.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmMappingUtils;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DfUtils;
import com.armedia.cmf.documentum.engine.common.DctmGroup;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
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
 * @author diego
 *
 */
public class DctmExportGroup extends DctmExportAbstract<IDfGroup> implements DctmGroup {

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (DctmExportGroup.HANDLERS_READY) { return; }
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
		DctmExportGroup.HANDLERS_READY = true;
	}

	private static boolean SPECIAL_GROUPS_READY = false;
	private static Set<String> SPECIAL_GROUPS = Collections.emptySet();

	private static synchronized void initSpecialGroups() {
		if (DctmExportGroup.SPECIAL_GROUPS_READY) { return; }
		String specialGroups = "";
		// TODO: Setting.SPECIAL_GROUPS.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(specialGroups);
		DctmExportGroup.SPECIAL_GROUPS = Collections.unmodifiableSet(new HashSet<String>(strTokenizer.getTokenList()));
		DctmExportGroup.SPECIAL_GROUPS_READY = true;
	}

	public static boolean isSpecialGroup(String group) {
		DctmExportGroup.initSpecialGroups();
		return DctmExportGroup.SPECIAL_GROUPS.contains(group);
	}

	/**
	 * This DQL will find all users for which this group is marked as the default group, and thus
	 * all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_GROUP = "SELECT u.user_name FROM dm_user u, dm_group g WHERE u.user_group_name = g.group_name AND g.r_object_id = '%s'";

	protected DctmExportGroup(DctmExportEngine engine) {
		super(engine, DctmObjectType.GROUP);
		DctmExportGroup.initHandlers();
		DctmExportGroup.initSpecialGroups();
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfGroup group) throws DfException {
		return group.getGroupName();
	}

	private Collection<IDfValue> getUsersWithDefaultGroup(IDfGroup group) throws DfException {
		IDfCollection resultCol = DfUtils.executeQuery(group.getSession(),
			String.format(DctmExportGroup.DQL_FIND_USERS_WITH_DEFAULT_GROUP, group.getObjectId().getId()),
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
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfGroup group, ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findRequirements(session, marshaled, group, ctx);

		String groupOwner = group.getOwnerName();
		if (!DctmMappingUtils.isMappableUser(session, groupOwner) && !DctmUserExporter.isSpecialUser(groupOwner)) {
			IDfUser owner = session.getUser(groupOwner);
			if (owner != null) {
				ret.add(owner);
			} else {
				throw new Exception(String.format(
					"Missing dependency for group [%s] - user [%s] not found (as group owner)", group.getGroupName(),
					groupOwner));
			}
		} else {
			this.log.warn(String.format("Skipping export of special user [%s] as the owner of group [%s]", groupOwner,
				group.getGroupName()));
		}

		String groupAdmin = group.getGroupAdmin();
		if (!DctmMappingUtils.isMappableUser(session, groupAdmin) && !DctmUserExporter.isSpecialUser(groupAdmin)) {
			IDfUser admin = session.getUser(groupAdmin);
			if (admin != null) {
				ret.add(admin);
			} else {
				throw new Exception(String.format(
					"Missing dependency for group [%s] - user [%s] not found (as group admin)", group.getGroupName(),
					groupAdmin));
			}
		} else {
			this.log.warn(String.format("Skipping export of special user [%s] as the admin of group [%s]", groupAdmin,
				group.getGroupName()));
		}

		StoredAttribute<IDfValue> usersNames = marshaled.getAttribute(DctmAttributes.USERS_NAMES);
		if (usersNames != null) {
			for (IDfValue v : usersNames) {
				String userName = v.asString();
				if (DctmUserExporter.isSpecialUser(userName)) {
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
				ret.add(member);
			}
		}

		StoredAttribute<IDfValue> groupsNames = marshaled.getAttribute(DctmAttributes.GROUPS_NAMES);
		if (groupsNames != null) {
			for (IDfValue v : groupsNames) {
				String groupName = v.asString();
				if (DctmExportGroup.isSpecialGroup(groupName)) {
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
				ret.add(member);
			}
		}
		return ret;
	}

	@Override
	protected Collection<IDfPersistentObject> findDependents(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfGroup group, ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findDependents(session, marshaled, group, ctx);

		// Avoid calling DQL twice
		StoredProperty<IDfValue> property = marshaled.getProperty(DctmGroup.USERS_WITH_DEFAULT_GROUP);
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
				throw new Exception(String.format(
					"WARNING: Missing dependency for group [%s] - user [%s] not found (as default group)",
					group.getGroupName(), v.asString()));
			}
			ret.add(user);
		}
		return ret;
	}

}