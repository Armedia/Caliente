package com.armedia.cmf.engine.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BooleanContext {
	protected final Map<String, Object> values;

	public BooleanContext(Map<String, Object> values) {
		this.values = new HashMap<String, Object>(values);
	}

	public Set<String> getValueNames() {
		return this.values.keySet();
	}

	public Object getValue(String name) {
		return this.values.get(name);
	}

	public int getValueCount() {
		return this.values.size();
	}

	public boolean hasName(String name) {
		return this.values.containsKey(name);
	}
}