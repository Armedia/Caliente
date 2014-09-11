/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.UUID;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsContent extends CmsObject<IDfContent> {

	public CmsContent() {
		super(CmsObjectType.CONTENT, IDfContent.class);
	}

	@Override
	protected String calculateLabel(IDfContent object) throws DfException {
		return UUID.randomUUID().toString();
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfContent user) throws DfException {
	}

	@Override
	protected IDfContent locateInCms(IDfSession session) throws DfException {
		// TODO Auto-generated method stub
		return null;
	}
}