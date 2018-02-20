package com.armedia.caliente.cli;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public class EnumValueFilter<E extends Enum<E>> extends OptionValueFilter {

	private final Class<E> klass;
	private final Set<E> included;
	private final String description;

	public EnumValueFilter(Class<E> enumClass) {
		this(enumClass, null);
	}

	public EnumValueFilter(Class<E> enumClass, Set<E> excluded) {
		Objects.requireNonNull(enumClass, "Must provide an enum class");
		if (!enumClass.isEnum()) { throw new IllegalArgumentException(
			String.format("The class %s is not an Enum", enumClass.getCanonicalName())); }
		this.klass = enumClass;
		Set<E> included = EnumSet.allOf(this.klass);
		if ((excluded != null) && !excluded.isEmpty()) {
			included.removeAll(excluded);
		}
		if (included
			.isEmpty()) { throw new IllegalArgumentException("No values are marked as allowed, this is illegal"); }
		Set<String> v = new TreeSet<>();
		for (E e : included) {
			v.add(e.name());
		}
		this.included = Tools.freezeSet(included);
		this.description = String.format("one of %s", v.toString());
	}

	@Override
	protected boolean checkValue(String value) {
		try {
			return this.included.contains(Enum.valueOf(this.klass, value));
		} catch (Exception e) {
			// Do nothing...
			if (this.log.isTraceEnabled()) {
				this.log.trace("Failed to decode the string [{}] as an enum of type [{}]", value,
					this.klass.getCanonicalName(), e);
			}
			return false;
		}
	}

	@Override
	public String getDefinition() {
		return this.description;
	}
}