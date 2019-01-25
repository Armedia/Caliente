package com.armedia.caliente.engine.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class MappingManager {

	private MappingManager() {
	}

	public static String generateMapping(String mapping, String name) {
		if (mapping != null) { return mapping; }
		if (name == null) { throw new IllegalArgumentException("Must provide a name to generate a mapping for"); }
		return String.format("cmf:%s", name.toLowerCase());
	}

	@SafeVarargs
	public static <E extends Supplier<String>> Map<String, E> createMappings(Class<E> mappableClass, E... mappables) {
		Map<String, E> m = new HashMap<>();
		if (mappables != null) {
			for (E a : mappables) {
				E b = m.put(a.get(), a);
				if (b != null) {
					throw new IllegalStateException(
						String.format("Duplicate mappings (%s) - %s and %s resolve to the same mapping name (%s|%s)",
							mappableClass.getCanonicalName(), a, b, a.get(), b.get()));
				}
			}
		}
		return m;
	}
}