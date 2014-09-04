/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.datastore.DataProperty;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class CmsFolder extends CmsObject<IDfFolder> {

	public CmsFolder() {
		super(CmsObjectType.FOLDER, IDfFolder.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfFolder user) throws DfException {
	}

	@Override
	protected IDfFolder locateInCms(IDfSession session) throws DfException {
		// TODO Auto-generated method stub
		return null;
	}
}