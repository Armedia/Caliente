/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.storage.StoredObject;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportFormat extends DctmImportDelegate<IDfFormat> {

	public DctmImportFormat(DctmImportEngine engine, StoredObject<IDfValue> storedObject) {
		super(engine, DctmObjectType.FORMAT, storedObject);
	}

	@Override
	protected String calculateLabel(IDfFormat format) throws DfException {
		return format.getName();
	}

	@Override
	protected void finalizeConstruction(IDfFormat object, boolean newObject, DctmImportContext context)
		throws DfException {
		if (newObject) {
			copyAttributeToObject(DctmAttributes.NAME, object);
		}
	}

	@Override
	protected IDfFormat locateInCms(DctmImportContext ctx) throws DfException {
		IDfSession session = ctx.getSession();
		IDfValue formatName = this.storedObject.getAttribute(DctmAttributes.NAME).getValue();
		return session.getFormat(formatName.asString());
	}
}