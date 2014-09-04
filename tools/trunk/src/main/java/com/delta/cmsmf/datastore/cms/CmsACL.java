/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;
import java.util.Set;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataType;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class CmsACL extends CmsObject<IDfACL> {

	private static final String USERS_WITH_DEFAULT_ACL = "usersWithDefaultACL";

	/**
	 * This DQL will find all users for which this ACL is marked as the default ACL,
	 * and thus all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_ACL = "SELECT u.user_name FROM dm_user u, dm_acl a WHERE u.acl_domain = a.owner_name AND u.acl_name = a.object_name AND a.r_object_id = ''%s''";

	public CmsACL() {
		super(CmsObjectType.ACL, IDfACL.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfACL acl) throws DfException {
		final String aclId = acl.getObjectId().getId();
		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL(String.format(CmsACL.DQL_FIND_USERS_WITH_DEFAULT_ACL, aclId));
		IDfCollection resultCol = dqlQry.execute(acl.getSession(), IDfQuery.EXEC_QUERY);
		try {
			DataProperty property = new DataProperty(CmsACL.USERS_WITH_DEFAULT_ACL, DataType.DF_STRING);
			while (resultCol.next()) {
				property.addValue(resultCol.getValueAt(0));
			}
			properties.add(property);
		} finally {
			closeQuietly(resultCol);
		}
	}

	@Override
	protected void applyDataProperties(Set<String> propertyNames, IDfACL acl) throws DfException {
		if (propertyNames.contains(CmsACL.USERS_WITH_DEFAULT_ACL)) {
			final IDfSession session = acl.getSession();
			final IDfValue objectName = getAttribute(DctmAttrNameConstants.OBJECT_NAME).getSingleValue();
			for (IDfValue value : getProperty(CmsACL.USERS_WITH_DEFAULT_ACL)) {

				// TODO: How do we decide if we should update the default ACL for this user?

				final IDfUser user = session.getUser(value.asString());
				if (user == null) {
					// TODO: Mark the error, but don't explode...
				} else {
					// Ok...so we relate this thing back to its owner as its internal ACL
					user.setDefaultACLEx(value.asString(), objectName.asString());
				}
			}
		}
	}

	@Override
	protected IDfACL locateInCms(IDfSession session) throws DfException {
		return null;
	}
}