/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataProperty;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

/**
 * @author diego
 *
 */
public class CmsFormat extends CmsObject<IDfFormat> {

	public CmsFormat() {
		super(CmsObjectType.FORMAT, IDfFormat.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfFormat user) throws DfException {
	}

	@Override
	protected DataAttribute getFilteredAttribute(boolean toCms, IDfPersistentObject object, IDfAttr attribute)
		throws DfException {
		return null;
	}

	@Override
	protected AttributeMode getAttributeMode(String attributeName) {
		return null;
	}
}