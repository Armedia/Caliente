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
		Objects.requireNonNull(type, "Must provide a valid account object type");
		Objects.requireNonNull(name, "Must provide a valid name");
		Objects.requireNonNull(access, "Must provide a valid level of access");
		this.type = type;
		this.name = name;
		this.access = access;
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
