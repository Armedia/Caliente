/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DfUtils;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.StoredObject;
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

	public DctmImportStore(DctmImportEngine engine, StoredObject<IDfValue> storedObject) {
		super(engine, DctmObjectType.STORE, storedObject);
	}

	@Override
	protected IDfStore newObject(DctmImportContext ctx) throws ImportException {
		// We can't create stores programmatically....so always explode
		IDfValue name = this.storedObject.getAttribute(DctmAttributes.NAME).getValue();
		throw new ImportException(
			String
			.format(
				"Store object creation is not supported - please contact an administrator and ask them to create a store named [%s]",
				name));
	}

	@Override
	protected String calculateLabel(IDfStore store) throws DfException {
		return store.getName();
	}

	@Override
	protected IDfStore locateInCms(DctmImportContext ctx) throws DfException {
		IDfValue name = this.storedObject.getAttribute(DctmAttributes.NAME).getValue();
		return DfUtils.getStore(ctx.getSession(), name.asString());
	}

	@Override
	protected boolean isSameObject(IDfStore store) throws DfException {
		IDfValue name = this.storedObject.getAttribute(DctmAttributes.NAME).getValue();
		return Tools.equals(name, store.getName());
	}

	@Override
	protected String generateSystemAttributesSQL(StoredObject<IDfValue> stored, IDfPersistentObject object)
		throws DfException {
		return null;
	}
}