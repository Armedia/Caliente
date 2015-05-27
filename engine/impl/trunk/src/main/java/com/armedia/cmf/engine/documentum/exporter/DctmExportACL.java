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
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.common.DctmACL;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfACL;
import com.armedia.cmf.storage.CmfActor;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.commons.utilities.FileNameTools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
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
			AttributeHandler h = DctmAttributeHandlers.getAttributeHandler(DctmObjectType.ACL, attr);
			prop.setValues(h.getExportableValues(acl, attr));
			cmfAcl.setProperty(prop);
		}

		Set<String> missingAccessors = new HashSet<String>();

		// Now do the accessors...
		final int accCount = acl.getValueCount(DctmAttributes.R_ACCESSOR_NAME);
		for (int i = 0; i < accCount; i++) {
			// We're only interested in the basic permissions
			final String accessorName = acl.getRepeatingString(DctmAttributes.R_ACCESSOR_NAME, i);
			final boolean group = acl.getRepeatingBoolean(DctmAttributes.R_IS_GROUP, i);
			final int accessorPermit = acl.getRepeatingInt(DctmAttributes.R_ACCESSOR_PERMIT, i);
			final String specialPermit = acl.getAccessorXPermitNames(i);

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

			final CmfActor.Type actorType = (group ? CmfActor.Type.GROUP : CmfActor.Type.USER);
			String s = DfUtils.decodeAccessPermission(accessorPermit);
			CmfActor accessor = new CmfActor(accessorName, actorType);
			accessor.addAction(s.toLowerCase());
			for (String x : FileNameTools.tokenize(specialPermit, ',')) {
				accessor.addAction(x.toLowerCase());
			}
			cmfAcl.addActor(accessor);
		}

		/*
		final IDfList permits = acl.getPermissions();
		final int permitCount = permits.getCount();
		for (int i = 0; i < permitCount; i++) {
			IDfPermit p = IDfPermit.class.cast(permits.get(i));
			final String accessorName = p.getAccessorName();

			Permission permission = Permission.NONE;

			final int permitType = p.getPermitType();
			switch (permitType) {
				case IDfPermit.DF_EXTENDED_PERMIT:
				case IDfPermit.DF_EXTENDED_RESTRICTION:
					// Extended permits aren't allowed
					continue;

				default:
					break;
			}

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

			ActorType accessorType = (group ? ActorType.GROUP : ActorType.USER);
			CmfActor accessor = cmfAcl.getAccessor(accessorType, accessorName);
			if (accessor == null) {
				accessor = new CmfActor(accessorName, accessorType);
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
		 */

		return cmfAcl;
	}
}