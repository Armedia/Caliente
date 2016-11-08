package com.armedia.caliente.engine.converter;

import java.util.HashMap;
import java.util.Map;

public final class MappingManager {

	public static interface Mappable {
		public String getMapping();
	}

	private MappingManager() {
	}

	public static String generateMapping(String mapping, String name) {
		if (mapping != null) { return mapping; }
		if (name == null) { throw new IllegalArgumentException("Must provide a name to generate a mapping for"); }
		return String.format("cmf:%s", name.toLowerCase());
	}

	public static <E extends Mappable> Map<String, E> createMappings(Class<E> mappableClass, E... mappables) {
		Map<String, E> m = new HashMap<String, E>();
		if (mappables != null) {
			for (E a : mappables) {
				E b = m.put(a.getMapping(), a);
				if (b != null) { throw new IllegalStateException(String.format(
					"Duplicate mappings (%s) - %s and %s resolve to the same mapping name (%s|%s)",
					mappableClass.getCanonicalName(), a, b, a.getMapping(), b.getMapping())); }
			}
		}
		return m;
	}
}