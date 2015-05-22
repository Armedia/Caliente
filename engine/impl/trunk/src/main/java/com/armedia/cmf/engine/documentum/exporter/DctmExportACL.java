/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.documentum.DctmAttributeHandlers;
import com.armedia.cmf.engine.documentum.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.common.DctmACL;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfACL;
import com.armedia.cmf.storage.CmfACL.AccessorType;
import com.armedia.cmf.storage.CmfAccessor;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfPermission;
import com.armedia.cmf.storage.CmfProperty;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportACL {

	private static final Logger LOG = LoggerFactory.getLogger(DctmExportACL.class);

	/**
	 * This DQL will find all users for which this ACL is marked as the default ACL, and thus all
	 * users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_ACL = "SELECT u.user_name FROM dm_user u, dm_acl a WHERE u.acl_domain = a.owner_name AND u.acl_name = a.object_name AND a.r_object_id = '%s'";

	public static CmfACL<IDfValue> calculateACL(final IDfACL acl) throws DfException, ExportException {
		if (acl == null) { return null; }
		final IDfSession session = acl.getSession();
		CmfACL<IDfValue> cmfAcl = new CmfACL<IDfValue>(acl.getObjectId().getId());

		final String aclId = acl.getObjectId().getId();
		IDfCollection resultCol = DfUtils.executeQuery(acl.getSession(),
			String.format(DctmExportACL.DQL_FIND_USERS_WITH_DEFAULT_ACL, aclId), IDfQuery.DF_EXECREAD_QUERY);
		CmfProperty<IDfValue> property = null;
		try {
			property = new CmfProperty<IDfValue>(DctmACL.USERS_WITH_DEFAULT_ACL, DctmDataType.DF_STRING.getStoredType());
			while (resultCol.next()) {
				IDfValue user = resultCol.getValueAt(0);
				property.addValue(user);
			}
			cmfAcl.setProperty(property);
		} finally {
			DfUtils.closeQuietly(resultCol);
		}

		final int attCount = acl.getAttrCount();
		for (int i = 0; i < attCount; i++) {
			final IDfAttr attr = acl.getAttr(i);
			final CmfDataType type = DctmDataType.fromAttribute(attr).getStoredType();
			final CmfProperty<IDfValue> prop = new CmfProperty<IDfValue>(attr.getName(), type, attr.isRepeating());
			// TODO: Only use this as the handler for the attributes which contain user/group
			// names...
			AttributeHandler h = DctmAttributeHandlers.USER_NAME_HANDLER;
			prop.setValues(h.getExportableValues(acl, attr));
			cmfAcl.setProperty(prop);
		}
		/*
			if (!group) {
				if (DctmMappingUtils.isMappableUser(session, name) || ctx.isSpecialUser(name)) {
					// User is mapped to a special user, so we shouldn't include it as a dependency
					// because it will be mapped on the target
					continue;
				}
			} else {
				if (ctx.isSpecialGroup(name)) {
					continue;
				}
			}

			if (DctmMappingUtils.SPECIAL_NAMES.contains(name)) {
				// This is a special name - non-existent per-se, but supported by the system
				// such as dm_owner, dm_group, dm_world
				continue;
			}

		 */

		Set<String> missingAccessors = new HashSet<String>();

		// Now do the accessors...
		final IDfList permits = acl.getPermissions();
		final int permitCount = permits.getCount();
		for (int i = 0; i < permitCount; i++) {
			IDfPermit p = IDfPermit.class.cast(permits.get(i));
			final String accessorName = p.getAccessorName();

			final int permitType = p.getPermitType();
			final String permit = p.getPermitValueString();
			boolean grant = true;
			boolean group = false;
			switch (permitType) {
				case IDfPermit.DF_REQUIRED_GROUP:
				case IDfPermit.DF_REQUIRED_GROUP_SET:
					group = true;
					break;

				case IDfPermit.DF_ACCESS_RESTRICTION:
				case IDfPermit.DF_APPLICATION_RESTRICTION:
				case IDfPermit.DF_EXTENDED_RESTRICTION:
					grant = false;
					break;

				default:
					break;
			}

			IDfPersistentObject o = (group ? session.getGroup(accessorName) : session.getUser(accessorName));
			if ((o == null) && !DctmMappingUtils.SPECIAL_NAMES.contains(accessorName)) {
				// Accessor not there, skip it...
				if (!missingAccessors.contains(accessorName)) {
					DctmExportACL.LOG.warn(String.format(
						"Missing dependency for ACL [%s] - %s [%s] not found (as ACL accessor)", acl.getObjectId()
						.getId(), (group ? "group" : "user"), accessorName));
					missingAccessors.add(accessorName);
				}
				continue;
			}

			AccessorType accessorType = (group ? AccessorType.GROUP : AccessorType.USER);
			CmfAccessor accessor = cmfAcl.getAccessor(accessorType, accessorName);
			if (accessor == null) {
				accessor = new CmfAccessor(accessorName, accessorType);
				cmfAcl.addAccessor(accessor);
			}

			String pt = null;
			switch (permitType) {
				case IDfPermit.DF_EXTENDED_PERMIT:
				case IDfPermit.DF_EXTENDED_RESTRICTION:
					pt = "dctm:extended";
					break;
				default:
					pt = "dctm:access";
					break;
			}

			accessor.addPermission(new CmfPermission(pt, permit, grant));
		}

		return cmfAcl;
	}
}