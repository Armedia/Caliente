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
package com.armedia.caliente.cli;

import java.util.List;

public final class OptionValue extends Option {
	private final OptionValues values;
	private final Option def;

	OptionValue(OptionValues values, Option def) {
		this.values = values;
		this.def = def.clone();
	}

	public Option getDefinition() {
		return this.def;
	}

	@Override
	public OptionValue clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getKey() {
		return this.def.getKey();
	}

	@Override
	public boolean isRequired() {
		return this.def.isRequired();
	}

	@Override
	public String getDescription() {
		return this.def.getDescription();
	}

	@Override
	public String getLongOpt() {
		return this.def.getLongOpt();
	}

	@Override
	public Character getShortOpt() {
		return this.def.getShortOpt();
	}

	@Override
	public Character getValueSep() {
		return this.def.getValueSep();
	}

	@Override
	public String getArgumentName() {
		return this.def.getArgumentName();
	}

	@Override
	public int getMinArguments() {
		return this.def.getMinArguments();
	}

	@Override
	public int getMaxArguments() {
		return this.def.getMaxArguments();
	}

	@Override
	public boolean isValueAllowed(String value) {
		return this.def.isValueAllowed(value);
	}

	@Override
	public OptionValueFilter getValueFilter() {
		return this.def.getValueFilter();
	}

	@Override
	public String getDefault() {
		return this.def.getDefault();
	}

	@Override
	public List<String> getDefaults() {
		return this.def.getDefaults();
	}

	public OptionValues getOptionValues() {
		return this.values;
	}

	public int getOccurrences() {
		return this.values.getOccurrences(this);
	}

	public boolean isPresent() {
		return this.values.isPresent(this);
	}

	public Boolean getBoolean() {
		return getBoolean(null);
	}

	public Boolean getBoolean(Boolean def) {
		return this.values.getBoolean(this, def);
	}

	public List<Boolean> getBooleans() {
		return this.values.getBooleans(this);
	}

	public Integer getInteger() {
		return getInteger(null);
	}

	public Integer getInteger(Integer def) {
		return this.values.getInteger(this, def);
	}

	public List<Integer> getIntegers() {
		return this.values.getIntegers(this);
	}

	public Long getLong() {
		return getLong(null);
	}

	public Long getLong(Long def) {
		return this.values.getLong(this, def);
	}

	public List<Long> getLongs() {
		return this.values.getLongs(this);
	}

	public Float getFloat() {
		return getFloat(null);
	}

	public Float getFloat(Float def) {
		return this.values.getFloat(this, def);
	}

	public List<Float> getFloats() {
		return this.values.getFloats(this);
	}

	public Double getDouble() {
		return getDouble(null);
	}

	public Double getDouble(Double def) {
		return this.values.getDouble(this, def);
	}

	public List<Double> getDoubles() {
		return this.values.getDoubles(this);
	}

	public String getString() {
		return getString(null);
	}

	public String getString(String def) {
		return this.values.getString(this, def);
	}

	public List<String> getStrings() {
		return this.values.getStrings(this);
	}

	public int getValueCount() {
		return this.values.getValueCount(this);
	}

	public boolean hasValues() {
		return this.values.hasValues(this);
	}
}