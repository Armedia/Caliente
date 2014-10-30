/**
 *
 */

package com.delta.cmsmf.cms;

import com.armedia.cmf.documentum.engine.DctmAttributeHandlers;
import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmFormat extends DctmPersistentObject<IDfFormat> {
	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (DctmFormat.HANDLERS_READY) { return; }
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FORMAT, DctmDataType.DF_STRING, DctmAttributes.NAME,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmFormat.HANDLERS_READY = true;
	}

	public DctmFormat() {
		super(IDfFormat.class);
		DctmFormat.initHandlers();
	}

	@Override
	protected String calculateLabel(IDfFormat format) throws DfException {
		return format.getName();
	}

	@Override
	protected void finalizeConstruction(IDfFormat object, boolean newObject, DctmTransferContext context)
		throws DfException {
		if (newObject) {
			copyAttributeToObject(DctmAttributes.NAME, object);
		}
	}

	@Override
	protected IDfFormat locateInCms(DctmTransferContext ctx) throws DfException {
		IDfSession session = ctx.getSession();
		IDfValue formatName = this.storedObject.getAttribute(DctmAttributes.NAME).getValue();
		return session.getFormat(formatName.asString());
	}
}