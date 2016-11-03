/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.storage.CmfObject;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportFormat extends DctmImportDelegate<IDfFormat> {

	public DctmImportFormat(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfFormat.class, DctmObjectType.FORMAT, storedObject);
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
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext ctx) throws DfException {
		return null;
	}

	@Override
	protected IDfFormat locateInCms(DctmImportContext ctx) throws DfException {
		IDfSession session = ctx.getSession();
		IDfValue formatName = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		return session.getFormat(formatName.asString());
	}
}