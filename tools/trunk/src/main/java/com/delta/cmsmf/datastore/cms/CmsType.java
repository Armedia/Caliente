/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataProperty;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

/**
 * @author diego
 *
 */
public class CmsType extends CmsObject<IDfType> {

	public CmsType() {
		super(CmsObjectType.TYPE, IDfType.class);
	}

	@Override
	protected void getDataProperties(Collection<DataProperty> properties, IDfType user) throws DfException {
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