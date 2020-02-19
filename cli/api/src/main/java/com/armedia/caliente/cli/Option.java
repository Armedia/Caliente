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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public abstract class Option implements PositionalValueSupport, Cloneable {

	public static final Pattern VALID_LONG = Pattern.compile("^[$\\w][-$\\w]*$");

	/**
	 * Allow letters, digits, '$' and '?' as short options
	 */
	public static final Pattern VALID_SHORT = Pattern.compile("^[$?\\p{Alnum}]$");

	public static final char DEFAULT_VALUE_SEP = ',';

	public static final int UNBOUNDED_MAX_VALUES = -1;

	public abstract String getKey();

	public abstract boolean isRequired();

	public abstract String getDescription();

	public abstract String getLongOpt();

	public abstract Character getShortOpt();

	public abstract Character getValueSep();

	public abstract OptionValueFilter getValueFilter();

	public abstract boolean isValueAllowed(String value);

	public abstract String getDefault();

	public abstract List<String> getDefaults();

	@Override
	public abstract Option clone();

	public final boolean isConflicting(Option other) {
		return Option.isConflicting(this, other);
	}

	public final boolean isEquivalent(Option other) {
		return Option.isEquivalent(this, other);
	}

	public final boolean isIdentical(Option other) {
		return Option.isIdentical(this, other);
	}

	/**
	 * <p>
	 * Checks for conflict between the options. Two options {@code a} and {@code b} are said to be
	 * in conflict if and only if the following methods return identical (as per
	 * {@link Object#equals(Object)} values:
	 * </p>
	 * <ul>
	 * <li>{@link #getShortOpt()}</li>
	 * <li>{@link #getLongOpt()}</li>
	 * </ul>
	 *
	 * @param a
	 * @param b
	 * @return {@code true} if there's a conflict between a and b, {@code false} otherwise
	 */
	public static boolean isConflicting(Option a, Option b) {
		if (a == b) { return true; }
		if ((a == null) || (b == null)) { return false; }
		if (!Objects.equals(a.getShortOpt(), b.getShortOpt())) { return false; }
		if (!Objects.equals(a.getLongOpt(), b.getLongOpt())) { return false; }
		return true;
	}

	/**
	 * <p>
	 * Checks for the equivalency between the options. Two options {@code a} and {@code b} are said
	 * to be equivalent if they're conflicting (as per {@link #isConflicting(Option, Option)}, and
	 * the following methods return identical (as per {@link Object#equals(Object)} values:
	 * </p>
	 * <ul>
	 * <li>{@link #getMinArguments()}</li>
	 * <li>{@link #getMaxArguments()}</li>
	 * <li>{@link #getValueSep()}</li>
	 * <li>{@link #getValueFilter()}</li>
	 * </ul>
	 *
	 * @param a
	 * @param b
	 * @return {@code true} if the options are equivalent, {@code false} otherwise
	 */
	public static boolean isEquivalent(Option a, Option b) {
		if (!Option.isConflicting(a, b)) { return false; }
		if (!Objects.equals(a.getMinArguments(), b.getMinArguments())) { return false; }
		if (!Objects.equals(a.getMaxArguments(), b.getMaxArguments())) { return false; }
		if (!Objects.equals(a.getValueSep(), b.getValueSep())) { return false; }
		if (!Objects.equals(a.getValueFilter(), b.getValueFilter())) { return false; }
		return true;
	}

	/**
	 * <p>
	 * Checks two options to see if they're identical in every respect. Two options {@code a} and
	 * {@code b} are said to be identical if they're equivalent (as per
	 * {@link #isEquivalent(Option, Option)}, and the following methods return identical (as per
	 * {@link Object#equals(Object)} values:
	 * </p>
	 * <ul>
	 * <li>{@link #isRequired()}</li>
	 * <li>{@link #getDescription()}</li>
	 * <li>{@link #getArgumentName()}</li>
	 * <li>{@link #getDefaults()}</li>
	 * </ul>
	 *
	 * @param a
	 * @param b
	 * @return {@code true} if the options are identical, {@code false} otherwise
	 */
	public static boolean isIdentical(Option a, Option b) {
		if (!Option.isEquivalent(a, b)) { return false; }
		if (!Objects.equals(a.isRequired(), b.isRequired())) { return false; }
		if (!Objects.equals(a.getDescription(), b.getDescription())) { return false; }
		if (!Objects.equals(a.getArgumentName(), b.getArgumentName())) { return false; }
		if (!Objects.equals(a.getDefaults(), b.getDefaults())) { return false; }
		return true;
	}

	static String calculateKey(Option def) {
		Objects.requireNonNull(def, "Must provide a option definition to calculate a key for");
		return Option.calculateKey(def.getLongOpt(), def.getShortOpt());
	}

	static String calculateKey(String longOpt, Character shortOpt) {
		return Option.calculateKey(longOpt, shortOpt != null ? shortOpt.toString() : null);
	}

	public static String calculateKey(String longOpt, String shortOpt) {
		if ((longOpt == null) && (shortOpt == null)) {
			throw new NullPointerException("Must provide at least one short or long option");
		}
		String opt = (longOpt != null ? longOpt : shortOpt.toString());
		String prefix = (longOpt != null ? "-" : "");
		return String.format("-%s%s", prefix, opt);
	}

	/**
	 * <p>
	 * Extracts the {@link Option} instance from the given wrapper. It'll return {@code null} if
	 * either the wrapper itself is {@code null}, or if its wrapped option is {@code null}.
	 * </p>
	 *
	 * @param wrapper
	 * @return the {@link Option} instance from the given wrapper
	 */
	public static Option unwrap(Supplier<Option> wrapper) {
		if (wrapper == null) { return null; }
		return wrapper.get();
	}

	/**
	 * <p>
	 * Produces a list of the {@link Option} instances wrapped by the given wrappers. All
	 * {@code null} values are filtered out and not preserved. If the array given is {@code null},
	 * the {@code null} value is returned.
	 * </p>
	 *
	 * @param wrappers
	 * @return a list of the {@link Option} instances wrapped by the given wrappers
	 */
	@SafeVarargs
	public static List<Option> unwrap(Supplier<Option>... wrappers) {
		if (wrappers == null) { return null; }
		return Option.unwrap(Arrays.asList(wrappers));
	}

	/**
	 * <p>
	 * Produces a list of the {@link Option} instances wrapped by the given wrappers. All
	 * {@code null} values are filtered out and not preserved. If the collection given is
	 * {@code null}, the {@code null} value is returned.
	 * </p>
	 *
	 * @param wrappers
	 * @return a list of the {@link Option} instances wrapped by the given wrappers
	 */
	public static List<Option> unwrap(Collection<Supplier<Option>> wrappers) {
		if (wrappers == null) { return null; }
		List<Option> l = new ArrayList<>(wrappers.size());
		wrappers.stream().map(Option::unwrap).filter(Objects::nonNull).forEach(l::add);
		return l;
	}
}