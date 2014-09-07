/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.cms.CmsAttributeHandlers.AttributeHandler;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class CmsFormat extends CmsObject<IDfFormat> {
	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsFormat.HANDLERS_READY) { return; }
		AttributeHandler handler = new AttributeHandler() {
			@Override
			public boolean includeInImport(IDfPersistentObject object, CmsAttribute attribute) throws DfException {
				return false;
			}
		};
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.FORMAT, CmsDataType.DF_STRING, DctmAttrNameConstants.NAME,
			handler);
		CmsFormat.HANDLERS_READY = true;
	}

	public CmsFormat() {
		super(CmsObjectType.FORMAT, IDfFormat.class);
		CmsFormat.initHandlers();
	}

	@Override
	protected void finalizeConstruction(IDfFormat object, boolean newObject) throws DfException {
		if (newObject) {
			copyAttributeToObject(DctmAttrNameConstants.NAME, object);
		}
	}

	@Override
	protected IDfFormat locateInCms(IDfSession session) throws DfException {
		IDfValue formatName = getAttribute(DctmAttrNameConstants.NAME).getValue();
		return session.getFormat(formatName.asString());
	}
}