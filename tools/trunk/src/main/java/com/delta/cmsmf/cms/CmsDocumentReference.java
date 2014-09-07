/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class CmsDocumentReference extends CmsObject<IDfDocument> {

	public CmsDocumentReference() {
		super(CmsObjectType.DOCUMENT_REFERENCE, IDfDocument.class);
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