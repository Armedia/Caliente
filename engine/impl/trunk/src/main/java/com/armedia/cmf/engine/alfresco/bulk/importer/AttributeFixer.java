package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.util.HashMap;
import java.util.Map;

public enum AttributeFixer {
	//
	cm_name() {
		@Override
		public String fixValue(String value) {
			return null;
		}
	},
	//
	;

	public abstract String fixValue(String value);

	private static Map<String, AttributeFixer> MAP;

	private static synchronized Map<String, AttributeFixer> getMap() {
		if (AttributeFixer.MAP != null) { return AttributeFixer.MAP; }
		Map<String, AttributeFixer> m = new HashMap<String, AttributeFixer>();
		for (AttributeFixer f : AttributeFixer.values()) {
			String name = f.name().replace('_', ':');
			AttributeFixer o = m.put(name, f);
			if (o != null) { throw new IllegalStateException(
				String.format("Attribute [%s] is mapped to by AttributeFixers %s and %s", name, f.name(), o.name())); }
		}
		return AttributeFixer.MAP;
	}

	public static AttributeFixer decode(String attName) {
		return AttributeFixer.getMap().get(attName);
	}
}