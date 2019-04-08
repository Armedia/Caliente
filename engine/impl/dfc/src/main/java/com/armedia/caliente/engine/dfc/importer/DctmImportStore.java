/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportStore extends DctmImportDelegate<IDfStore> {

	public DctmImportStore(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfStore.class, DctmObjectType.STORE, storedObject);
	}

	@Override
	protected IDfStore newObject(DctmImportContext ctx) throws ImportException {
		// We can't create stores programmatically....so always explode
		IDfValue name = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		throw new ImportException(String.format(
			"CmfStore object creation is not supported - please contact an administrator and ask them to create a store named [%s]",
			name));
	}

	@Override
	protected String calculateLabel(IDfStore store) throws DfException {
		return store.getName();
	}

	@Override
	protected IDfStore locateInCms(DctmImportContext ctx) throws DfException {
		IDfValue name = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		return DfcUtils.getStore(ctx.getSession(), name.asString());
	}

	@Override
	protected boolean isSameObject(IDfStore store, DctmImportContext ctx) throws DfException {
		IDfValue name = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		return Tools.equals(name, store.getName());
	}

	@Override
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext context) throws DfException {
		return null;
	}
}