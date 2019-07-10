/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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