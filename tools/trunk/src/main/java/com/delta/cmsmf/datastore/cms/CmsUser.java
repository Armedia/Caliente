/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.datastore.DfValueFactory;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

/**
 * @author diego
 *
 */
public class CmsUser extends CmsObject<IDfUser> {

	private static final String PROP_INTERNAL_ACL = "doesUserHaveInternalACL";

	public CmsUser() {
		super(CmsObjectType.USER, IDfUser.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfUser user) throws DfException {
		IDfACL acl = user.getSession().getACL(user.getACLDomain(), user.getACLName());
		properties.add(new DataProperty(CmsUser.PROP_INTERNAL_ACL, DataType.DF_BOOLEAN, false, DfValueFactory
			.newBooleanValue(acl.isInternal())));
	}

	@Override
	protected DataAttribute getFilteredAttribute(boolean toCms, IDfPersistentObject object, IDfAttr attribute)
		throws DfException {
		return null;
	}

	@Override
	protected AttributeMode getAttributeMode(String attributeName) {
		return null;
	}
}