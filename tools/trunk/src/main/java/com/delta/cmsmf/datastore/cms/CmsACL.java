/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataType;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class CmsACL extends CmsObject<IDfACL> {

	private static final String USER_INTERNAL_ACL_OWNER = "userInternalAclOwner";

	public CmsACL() {
		super(CmsObjectType.ACL, IDfACL.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfACL acl) throws DfException {
		if (acl.isInternal()) {
			final IDfValue ownerName = getAttribute(DctmAttrNameConstants.OWNER_NAME).getSingleValue();
			final IDfSession session = acl.getSession();
			IDfUser user = session.getUser(ownerName.asString());
			if (user != null) {
				final IDfValue objectName = getAttribute(DctmAttrNameConstants.OBJECT_NAME).getSingleValue();
				if (Tools.equals(ownerName.asString(), user.getACLDomain())
					&& Tools.equals(objectName.asString(), user.getACLName())) {
					// If this ACL is a user's internal ACL, then we store the name of that user
					setProperty(new DataProperty(CmsACL.USER_INTERNAL_ACL_OWNER, DataType.DF_STRING, ownerName));
				}
			}
		}
	}

	@Override
	protected IDfACL locateInCms(IDfSession session) throws DfException {
		return null;
	}
}