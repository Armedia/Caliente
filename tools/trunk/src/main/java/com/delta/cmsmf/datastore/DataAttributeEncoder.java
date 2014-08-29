package com.delta.cmsmf.datastore;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

public class DataAttributeEncoder {

	public DataAttribute encode(IDfPersistentObject object, IDfAttr attribute) throws DfException {
		return new DataAttribute(object, attribute);
	}

}