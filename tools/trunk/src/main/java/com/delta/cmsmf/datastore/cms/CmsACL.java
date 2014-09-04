/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.datastore.DataProperty;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class CmsACL extends CmsObject<IDfACL> {

	public CmsACL() {
		super(CmsObjectType.ACL, IDfACL.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfACL user) throws DfException {
	}

	@Override
	protected IDfACL locateInCms(IDfSession session) throws DfException {
		// TODO Auto-generated method stub
		return null;
	}
}