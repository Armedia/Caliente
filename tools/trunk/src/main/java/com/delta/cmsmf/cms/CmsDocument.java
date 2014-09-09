/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsDocument extends CmsObject<IDfDocument> {

	public CmsDocument() {
		super(CmsObjectType.DOCUMENT, IDfDocument.class);
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfDocument document) throws DfException {
	}

	@Override
	protected IDfDocument locateInCms(IDfSession session) throws DfException {
		return null;
	}
}