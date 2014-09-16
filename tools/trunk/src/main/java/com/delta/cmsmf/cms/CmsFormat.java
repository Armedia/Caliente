/**
 *
 */

package com.delta.cmsmf.cms;

import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsFormat extends CmsObject<IDfFormat> {
	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsFormat.HANDLERS_READY) { return; }
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.FORMAT, CmsDataType.DF_STRING, CmsAttributes.NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsFormat.HANDLERS_READY = true;
	}

	public CmsFormat() {
		super(IDfFormat.class);
		CmsFormat.initHandlers();
	}

	@Override
	protected String calculateLabel(IDfFormat format) throws DfException {
		return format.getName();
	}

	@Override
	protected void finalizeConstruction(IDfFormat object, boolean newObject, CmsTransferContext context)
		throws DfException {
		if (newObject) {
			copyAttributeToObject(CmsAttributes.NAME, object);
		}
	}

	@Override
	protected IDfFormat locateInCms(CmsTransferContext ctx) throws DfException {
		IDfSession session = ctx.getSession();
		IDfValue formatName = getAttribute(CmsAttributes.NAME).getValue();
		return session.getFormat(formatName.asString());
	}
}