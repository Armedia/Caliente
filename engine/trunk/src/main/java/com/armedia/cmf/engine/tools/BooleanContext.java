package com.armedia.cmf.engine.tools;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class BooleanContext {
	private final Map<String, Object> values;

	public BooleanContext(Map<String, Object> values) {
		this.values = Collections.unmodifiableMap(values);
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