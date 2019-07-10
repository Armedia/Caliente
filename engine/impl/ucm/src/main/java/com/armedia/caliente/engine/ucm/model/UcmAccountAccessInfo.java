/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.ucm.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.commons.utilities.Tools;

public class UcmAccountAccessInfo {

	private static final Pattern PARSER = Pattern.compile("^(account|role),(.*),(1|3|7|15)$", Pattern.CASE_INSENSITIVE);

	public static enum Type {
		//
		ACCOUNT, //
		ROLE, //
		//
		;

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum Access {
		//
		NONE(0), //
		READ(1), //
		READ_WRITE(3), //
		READ_WRITE_DELETE(7), //
		ADMINISTRATIVE(15),
		//
		;

		private static final Map<String, Access> STRINGS;
		private static final Map<Integer, Access> INTEGERS;

		static {
			Map<String, Access> s = new HashMap<>();
			for (Access a : Access.values()) {
				s.put(a.codeStr, a);
			}
			STRINGS = Tools.freezeMap(s);
			Map<Integer, Access> i = new HashMap<>();
			for (Access a : Access.values()) {
				i.put(a.code, a);
			}
			INTEGERS = Tools.freezeMap(i);
		}

		private final int code;
		private final String codeStr;

		private Access(int code) {
			this.code = code;
			this.codeStr = String.valueOf(code);
		}

		public int getCode() {
			return this.code;
		}

		@Override
		public String toString() {
			return this.codeStr;
		}

		public static Access decode(String str) {
			Access a = Access.STRINGS.get(str);
			if (a == null) { throw new IllegalArgumentException(String.format("Invalid access level [%s]", str)); }
			return a;
		}

		public static Access decode(int i) {
			Access a = Access.INTEGERS.get(i);
			if (a == null) { throw new IllegalArgumentException(String.format("Invalid access level [%d]", i)); }
			return a;
		}
	}

	private final Type type;
	private final String name;
	private final Access access;

	public UcmAccountAccessInfo(Type type, String name, Access access) {
		this.type = Objects.requireNonNull(type, "Must provide a valid account object type");
		this.name = Objects.requireNonNull(name, "Must provide a valid name");
		this.access = Objects.requireNonNull(access, "Must provide a valid level of access");
	}

	public Type getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}

	public Access getAccess() {
		return this.access;
	}

	public static UcmAccountAccessInfo parse(String str) {
		Objects.requireNonNull(str, "Must provide a string to parse");
		Matcher m = UcmAccountAccessInfo.PARSER.matcher(str);
		if (!m.matches()) { return null; }
		Type type = Type.valueOf(m.group(1).toUpperCase());
		String name = m.group(2);
		Access access = Access.decode(m.group(3));
		return new UcmAccountAccessInfo(type, name, access);
	}
}
