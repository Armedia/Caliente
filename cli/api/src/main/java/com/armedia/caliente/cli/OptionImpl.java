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
package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.armedia.commons.utilities.Tools;

public final class OptionImpl extends Option implements Cloneable {

	private boolean required = false;
	private String description = null;
	private Character shortOpt = null;
	private String longOpt = null;
	private int minArguments = 0;
	private int maxArguments = 0;
	private String argumentName = null;
	private Character valueSep = null;
	private OptionValueFilter valueFilter = null;
	private final List<String> defaults = new ArrayList<>();

	private String key = null;

	public OptionImpl() {
	}

	public OptionImpl(char shortOpt) {
		this(shortOpt, null);
	}

	public OptionImpl(String longOpt) {
		this(null, longOpt);
	}

	public OptionImpl(Character shortOpt, String longOpt) {
		if (shortOpt != null) {
			setShortOpt(shortOpt);
		}
		if (longOpt != null) {
			setLongOpt(longOpt);
		}
	}

	public OptionImpl(Option other) {
		if (other != null) {
			this.required = other.isRequired();
			this.description = other.getDescription();
			this.shortOpt = other.getShortOpt();
			this.longOpt = other.getLongOpt();
			this.minArguments = other.getMinArguments();
			this.maxArguments = other.getMaxArguments();
			this.argumentName = other.getArgumentName();
			this.valueSep = other.getValueSep();
			this.valueFilter = other.getValueFilter();
			List<String> defaults = other.getDefaults();
			if (defaults != null) {
				this.defaults.addAll(defaults);
			}
			this.key = other.getKey();
		}
	}

	@Override
	public OptionImpl clone() {
		return new OptionImpl(this);
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public int getMinArguments() {
		return this.minArguments;
	}

	public OptionImpl setMinArguments(int count) {
		this.minArguments = Math.max(0, count);
		if ((this.minArguments > 0) && (this.maxArguments >= 0)) {
			// if the new minimum value is higher than the current maximum,
			// then we grow the maximum to match
			this.maxArguments = Math.max(this.minArguments, this.maxArguments);
		}
		return this;
	}

	@Override
	public int getMaxArguments() {
		return this.maxArguments;
	}

	public OptionImpl setMaxArguments(int count) {
		this.maxArguments = Math.max(Option.UNBOUNDED_MAX_VALUES, count);
		if ((this.minArguments > 0) && (this.maxArguments >= 0)) {
			// if the current minimum value is higher than the new maximum,
			// then we shrink the minimum to match
			this.minArguments = Math.min(this.minArguments, this.maxArguments);
		}
		return this;
	}

	public OptionImpl setArgumentLimits(int count) {
		return setArgumentLimits(count, count);
	}

	public OptionImpl setArgumentLimits(int min, int max) {
		// Ensure both values are in the expected ranges
		min = Math.max(0, min);
		max = Math.max(Option.UNBOUNDED_MAX_VALUES, max);

		// Ensure that the maximum is never below the minimum
		if ((min != max) && (min != 0) && (max >= 0)) {
			max = Math.max(min, max);
		}

		this.minArguments = min;
		this.maxArguments = max;
		return this;
	}

	@Override
	public String getArgumentName() {
		return this.argumentName;
	}

	public OptionImpl setArgumentName(String argName) {
		this.argumentName = argName;
		return this;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}

	public OptionImpl setRequired(boolean required) {
		this.required = required;
		return this;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	public OptionImpl setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getLongOpt() {
		return this.longOpt;
	}

	public OptionImpl setLongOpt(String longOpt) {
		if (longOpt != null) {
			boolean valid = Option.VALID_LONG.matcher(longOpt).matches();
			if (valid) {
				valid &= (longOpt.length() > 1);
			}
			if (!valid) {
				throw new IllegalArgumentException(String.format("The long option value [%s] is not valid", longOpt));
			}
		}
		this.longOpt = longOpt;
		this.key = Option.calculateKey(this);
		return this;
	}

	@Override
	public Character getShortOpt() {
		return this.shortOpt;
	}

	public OptionImpl setShortOpt(Character shortOpt) {
		if (shortOpt != null) {
			boolean valid = Option.VALID_SHORT.matcher(shortOpt.toString()).matches();
			if (!valid) {
				throw new IllegalArgumentException(String.format("The short option value [%s] is not valid", shortOpt));
			}
		}
		this.shortOpt = shortOpt;
		this.key = Option.calculateKey(this);
		return this;
	}

	@Override
	public Character getValueSep() {
		return this.valueSep;
	}

	public OptionImpl setValueSep(Character valueSep) {
		this.valueSep = Objects.requireNonNull(valueSep, "Must provide a non-null value separator");
		return this;
	}

	@Override
	public boolean isValueAllowed(String value) {
		return (this.valueFilter != null ? this.valueFilter.isAllowed(value) : true);
	}

	@Override
	public OptionValueFilter getValueFilter() {
		return this.valueFilter;
	}

	public OptionImpl setValueFilter(OptionValueFilter valueFilter) {
		this.valueFilter = valueFilter;
		return this;
	}

	@Override
	public String getDefault() {
		return this.defaults.isEmpty() ? null : this.defaults.get(0);
	}

	public OptionImpl setDefault(Object value) {
		return setDefaults(Tools.toString(value));
	}

	@Override
	public List<String> getDefaults() {
		return this.defaults.isEmpty() ? null : this.defaults;
	}

	public OptionImpl setDefaults(Collection<String> defaults) {
		this.defaults.clear();
		if (defaults != null) {
			defaults.stream().filter(Objects::nonNull).forEach(this.defaults::add);
		}
		return this;
	}

	public OptionImpl setDefaults(String... defaults) {
		if (defaults != null) {
			setDefaults(Arrays.asList(defaults));
		} else {
			this.defaults.clear();
		}
		return this;
	}

	public static OptionImpl cast(Option o) {
		if (OptionImpl.class.isInstance(o)) { return OptionImpl.class.cast(o); }
		return null;
	}

	public static OptionImpl cast(Supplier<Option> o) {
		return OptionImpl.cast(Option.unwrap(o));
	}

	@Override
	public String toString() {
		return String.format(
			"OptionImpl [key=%s, required=%s, shortOpt=%s, longOpt=%s, description=%s, minArguments=%d, maxArguments=%d, argumentName=%s, minArguments=%s, valueSep=%s, defaults=%s]",
			this.key, this.required, this.shortOpt, this.longOpt, this.description, this.minArguments,
			this.maxArguments, this.argumentName, this.minArguments, this.valueSep, this.defaults);
	}

	public static <E extends Enum<E>> OptionImpl initOptionName(E e, OptionImpl p) {
		Objects.requireNonNull(p, "Must provide a OptionImpl whose option to initialize");
		if (e == null) { return p; }
		final String name = e.name();
		if (name.length() == 1) {
			// If we decide that the name of the option will be a single character, we use that
			p.setShortOpt(name.charAt(0));
		} else if (p.getLongOpt() == null) {
			// Otherwise, use the name replacing underscores with dashes
			p.setLongOpt(name.replace('_', '-'));
		}
		return p;
	}
}