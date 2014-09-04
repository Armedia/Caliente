/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.datastore.DataProperty;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class CmsType extends CmsObject<IDfType> {

	public CmsType() {
		super(CmsObjectType.TYPE, IDfType.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfType user) throws DfException {
	}

	@Override
	protected IDfType locateInCms(IDfSession session) throws DfException {
		return null;
	}
}