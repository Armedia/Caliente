package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.storage.StoredDataType;

public class ShptValue {

	public StoredDataType getType() {
		return null;
	}

	public boolean isList() {
		return false;
	}

	public int getValueCount() {
		return 0;
	}

	public Object getValue() {
		return getValue(0);
	}

	public Object getValue(int pos) {
		return null;
	}

	public void addValue(Object value) {

	}

	public void setValue(Object... value) {

	}

	public void clearValue() {

	}
}