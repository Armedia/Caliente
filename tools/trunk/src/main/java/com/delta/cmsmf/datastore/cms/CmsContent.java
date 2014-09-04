/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.datastore.DataProperty;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class CmsContent extends CmsObject<IDfContent> {

	public CmsContent() {
		super(CmsObjectType.CONTENT, IDfContent.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfContent user) throws DfException {
	}

	@Override
	protected IDfContent locateInCms(IDfSession session) throws DfException {
		// TODO Auto-generated method stub
		return null;
	}
}