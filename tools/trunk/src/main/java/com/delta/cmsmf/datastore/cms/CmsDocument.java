/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class CmsDocument extends CmsObject<IDfDocument> {

	public CmsDocument() {
		super(CmsObjectType.DOCUMENT, IDfDocument.class);
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfDocument user) throws DfException {
	}

	@Override
	protected IDfDocument locateInCms(IDfSession session) throws DfException {
		// TODO Auto-generated method stub
		return null;
	}
}