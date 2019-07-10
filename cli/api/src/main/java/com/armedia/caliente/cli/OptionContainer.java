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

import java.util.Collection;

public interface OptionContainer extends Iterable<Option> {

	/**
	 * Get this container's description
	 *
	 * @return this container's description
	 */
	public String getDescription();

	/**
	 * Set a description for this container
	 *
	 * @param description
	 */
	public OptionContainer setDescription(String description);

	/**
	 * Returns this scheme's given name.
	 *
	 * @return this scheme's given name
	 */
	public String getName();

	/**
	 * Find the option in this scheme which matches the given long option
	 *
	 * @param longOpt
	 *            the long option
	 * @return the option in this scheme which matches the given long option
	 */
	public Option getOption(String longOpt);

	/**
	 * Find the option in this scheme which matches the given short option
	 *
	 * @param shortOpt
	 *            the short option
	 * @return the option in this scheme which matches the given short option
	 */
	public Option getOption(Character shortOpt);

	/**
	 * Returns a {@link Collection} containing the options defined in this scheme. The collection
	 * returned is independent and changes to it will not reflect on this OptionScheme instance.
	 *
	 * @return a {@link Collection} containing the options defined in this scheme
	 */
	public Collection<Option> getOptions();

	/**
	 * Returns a collection of the options in this scheme that have required flag set (as determined
	 * by {@link Option#isRequired()}). The collection returned is a copy, and changes to it do not
	 * affect this scheme's contents.
	 *
	 * @return the options in this scheme that have required flag set
	 */
	public Collection<Option> getRequiredOptions();

	/**
	 * Returns the number of options in this scheme that have required flag set (as determined by
	 * {@link Option#isRequired()}).
	 *
	 * @return the number of options in this scheme that have required flag set
	 */
	public int getRequiredOptionCount();

	/**
	 * Returns the number of options defined in this scheme.
	 *
	 * @return the number of options defined in this scheme.
	 */
	public int getOptionCount();

	/**
	 * Returns {@code true} if this scheme contains an option that uses the given short option,
	 * {@code false} otherwise.
	 *
	 * @param shortOpt
	 * @return {@code true} if this scheme contains an option that uses the given short option,
	 *         {@code false} otherwise.
	 */
	public boolean hasOption(Character shortOpt);

	/**
	 * Returns {@code true} if this scheme contains an option that uses the given long option,
	 * {@code false} otherwise.
	 *
	 * @param longOpt
	 * @return {@code true} if this scheme contains an option that uses the given long option,
	 *         {@code false} otherwise.
	 */
	public boolean hasOption(String longOpt);

	public int hasOption(Option option);

	/**
	 * Returns an integer between 0 and 3 where the low bit is the presence indicator for the short
	 * option, and the high bit is the presence indicator for the long option. This method does not
	 * take into account whether both flags are associated to the same option. Use
	 * {@link #findCollisions(Character, String)} for that.
	 *
	 * @param shortOpt
	 * @param longOpt
	 * @return an integer between 0 and 3 where the low bit is the presence indicator for the short
	 *         option, and the high bit is the presence indicator for the long option.
	 */
	public int hasOption(Character shortOpt, String longOpt);

	/**
	 * Returns {@code true} if and only if this option scheme contains an option that equivalent (as
	 * per {@link Option#isEquivalent(Option, Option)}) to the given option, {@code false}
	 * otherwise.
	 *
	 * @param option
	 * @return {@code true} if and only if this option scheme contains an option that exactly
	 *         matches the given option in both long and short options, {@code false} otherwise.
	 *
	 */
	public boolean hasEquivalentOption(Option option);

	/**
	 * Returns the number of options already in this scheme that would collide with the given option
	 * based on short or long options. This means that only 3 values can be returned: 0, 1, or 2.
	 *
	 * @param option
	 * @return the number of options already in this scheme that would collide with the given option
	 *         based on short or long options
	 */
	public int countCollisions(Option option);

	/**
	 * Returns the options already in this scheme that would collide with the given option based on
	 * short or long options. If no collisions are found, {@code null} is returned. This means that
	 * the collection may contain either 1 or 2 elements.
	 *
	 * @param option
	 *            the option to check for
	 * @return the options already in this scheme that would collide with the given option based on
	 *         short or long options, or {@code null} if none collide
	 */
	public Collection<Option> findCollisions(Option option);

	/**
	 * Returns the options already in this scheme that would collide with the given option, based on
	 * short or long options. If no collisions are found, {@code null} is returned.
	 *
	 * @param options
	 *            the options to check for
	 * @return the options already in this scheme that would collide with the given option, based on
	 *         short or long options, or {@code null} if none collide
	 */
	public <O extends Option> Collection<Option> findCollisions(Iterable<O> options);

	/**
	 * Returns the Option already in this scheme that would collide with the given short or long
	 * options. If no collisions are found, {@code null} is returned. This means that the collection
	 * may contain either 1 or 2 elements.
	 *
	 * @param shortOpt
	 *            the short option to check for
	 * @param longOpt
	 *            the long option to check for
	 * @return the Option already in this scheme that would collide with the given short or long
	 *         options, or {@code null} if none collide
	 */
	public Collection<Option> findCollisions(Character shortOpt, String longOpt);
}