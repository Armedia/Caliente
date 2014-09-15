/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;

import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsContent extends CmsObject<IDfContent> {

	public CmsContent() {
		super(IDfContent.class);
	}

	@Override
	protected String calculateLabel(IDfContent content) throws DfException {
		return content.getObjectId().getId();
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfContent user) throws DfException {
	}

	@Override
	protected IDfContent locateInCms(CmsTransferContext ctx) throws DfException {
		// TODO Auto-generated method stub
		return null;
	}
}