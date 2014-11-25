/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmMappingUtils;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DfUtils;
import com.armedia.cmf.documentum.engine.DfValueFactory;
import com.armedia.cmf.documentum.engine.common.DctmACL;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportACL extends DctmExportAbstract<IDfACL> implements DctmACL {

	/**
	 * This DQL will find all users for which this ACL is marked as the default ACL, and thus all
	 * users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_ACL = "SELECT u.user_name FROM dm_user u, dm_acl a WHERE u.acl_domain = a.owner_name AND u.acl_name = a.object_name AND a.r_object_id = '%s'";

	protected DctmExportACL(DctmExportEngine engine) {
		super(engine, DctmObjectType.ACL);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfACL acl) throws DfException {
		return String.format("%s::%s", acl.getDomain(), acl.getObjectName());
	}

	@Override
	protected void getDataProperties(Collection<StoredProperty<IDfValue>> properties, IDfACL acl) throws DfException {
		final String aclId = acl.getObjectId().getId();
		IDfCollection resultCol = DfUtils.executeQuery(acl.getSession(),
			String.format(DctmExportACL.DQL_FIND_USERS_WITH_DEFAULT_ACL, aclId), IDfQuery.DF_EXECREAD_QUERY);
		StoredProperty<IDfValue> property = null;
		try {
			property = new StoredProperty<IDfValue>(DctmACL.USERS_WITH_DEFAULT_ACL,
				DctmDataType.DF_STRING.getStoredType());
			while (resultCol.next()) {
				property.addValue(resultCol.getValueAt(0));
			}
			properties.add(property);
		} finally {
			DfUtils.closeQuietly(resultCol);
		}

		StoredProperty<IDfValue> accessors = new StoredProperty<IDfValue>(DctmACL.ACCESSORS,
			DctmDataType.DF_STRING.getStoredType(), true);
		StoredProperty<IDfValue> permitTypes = new StoredProperty<IDfValue>(DctmACL.PERMIT_TYPE,
			DctmDataType.DF_INTEGER.getStoredType(), true);
		StoredProperty<IDfValue> permitValues = new StoredProperty<IDfValue>(DctmACL.PERMIT_VALUE,
			DctmDataType.DF_STRING.getStoredType(), true);
		IDfList permits = acl.getPermissions();
		final int permitCount = permits.getCount();
		final IDfSession session = acl.getSession();
		Set<String> missingAccessors = new HashSet<String>();
		for (int i = 0; i < permitCount; i++) {
			IDfPermit p = IDfPermit.class.cast(permits.get(i));
			// First, validate the accessor
			final String accessor = p.getAccessorName();
			final boolean group;
			switch (p.getPermitType()) {
				case IDfPermit.DF_REQUIRED_GROUP:
				case IDfPermit.DF_REQUIRED_GROUP_SET:
					group = true;
					break;

				default:
					group = false;
					break;
			}

			IDfPersistentObject o = (group ? session.getGroup(accessor) : session.getUser(accessor));
			if ((o == null) && !DctmMappingUtils.SPECIAL_NAMES.contains(accessor)) {
				// Accessor not there, skip it...
				if (!missingAccessors.contains(accessor)) {
					this.log.warn(String.format(
						"Missing dependency for ACL [%s] - %s [%s] not found (as ACL accessor)",
						calculateLabel(session, acl), (group ? "group" : "user"), accessor));
					missingAccessors.add(accessor);
				}
				continue;
			}

			accessors.addValue(DfValueFactory.newStringValue(DctmMappingUtils.substituteMappableUsers(acl, accessor)));
			permitTypes.addValue(DfValueFactory.newIntValue(p.getPermitType()));
			permitValues.addValue(DfValueFactory.newStringValue(p.getPermitValueString()));
		}
		properties.add(accessors);
		properties.add(permitValues);
		properties.add(permitTypes);
	}

	@Override
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfACL acl, DctmExportContext ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findRequirements(session, marshaled, acl, ctx);
		final int count = acl.getAccessorCount();
		for (int i = 0; i < count; i++) {
			final String name = acl.getAccessorName(i);
			final boolean group = acl.isGroup(i);

			if (!group) {
				if (DctmMappingUtils.isMappableUser(session, name)) {
					// User is mapped to a special user, so we shouldn't include it as a dependency
					// because it will be mapped on the target
					continue;
				}
			}

			if (DctmMappingUtils.SPECIAL_NAMES.contains(name)) {
				// This is a special name - non-existent per-se, but supported by the system
				// such as dm_owner, dm_group, dm_world
				continue;
			}

			final IDfPersistentObject obj = (group ? session.getGroup(name) : session.getUser(name));
			if (obj == null) {
				this.log.warn(String.format("Missing dependency for ACL [%s] - %s [%s] not found (as ACL accessor)",
					acl.getObjectName(), (group ? "group" : "user"), name));
				continue;
			}
			ret.add(obj);
		}

		// Do the owner
		final String owner = acl.getDomain();
		if (DctmMappingUtils.isMappableUser(session, owner)) {
			this.log.warn(String.format("Skipping export of special user [%s]", owner));
		} else {
			IDfUser user = session.getUser(owner);
			if (user == null) { throw new Exception(String.format(
				"Missing dependency for ACL [%s:%s] - user [%s] not found (as ACL domain)", owner, acl.getObjectName(),
				owner)); }
			ret.add(user);
		}
		return ret;
	}
}