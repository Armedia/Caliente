/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsDocument extends CmsObject<IDfDocument> {

	public CmsDocument() {
		super(CmsObjectType.DOCUMENT, IDfDocument.class);
	}

	@Override
	protected String calculateLabel(IDfDocument document) throws DfException {
		IDfId id = document.getFolderId(0);
		String path = "(unknown)";
		if (id != null) {
			IDfFolder f = IDfFolder.class.cast(document.getSession().getObject(id));
			if (f != null) {
				path = f.getFolderPath(0);
			}
		}
		return String.format("%s/%s", path, document.getObjectName());
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfDocument document) throws DfException {
	}

	@Override
	protected IDfDocument locateInCms(IDfSession session) throws DfException {
		return null;
	}
}