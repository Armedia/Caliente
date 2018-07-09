package com.armedia.caliente.cli;

import java.util.Collection;

import com.armedia.caliente.cli.exception.DuplicateOptionException;

public interface OptionGroup extends OptionContainer {

	/**
	 * Adds all the options from the given group
	 *
	 * @param optionGroup
	 *            the option group from which to add the options
	 * @throws IllegalArgumentException
	 *             if the given option collides with any already-existing options (you can check
	 *             with {@link #findCollisions(OptionGroup)})
	 */
	public OptionGroup addGroup(OptionGroup optionGroup) throws DuplicateOptionException;

	/**
	 * Returns the options incoming from the given option group that would collide with existing
	 * collisions in this group, based on short or long options. If no collisions are found,
	 * {@code null} is returned.
	 *
	 * @param optionGroup
	 *            the option group whose options should be checked for
	 * @return the options in the incoming group that would collide with options already in this
	 *         group based on short or long options, or {@code null} if none collide
	 */
	public Collection<Option> findCollisions(OptionGroup optionGroup);

	/**
	 * Adds the given option to this option group.
	 *
	 * @param option
	 *            the option to add
	 * @throws IllegalArgumentException
	 *             if the given option collides with any already-existing options (you can check
	 *             with {@link #hasOption(Character)}, {@link #hasOption(String)}, or
	 *             {@link #countCollisions(Option)}
	 */
	public OptionGroup add(Option option) throws DuplicateOptionException;

	/**
	 * See {@link #add(Option)}.
	 *
	 * @param option
	 *            the wrapped option to add
	 * @throws IllegalArgumentException
	 *             if the given wrapped option collides with any already-existing options (you can
	 *             check with {@link #hasOption(Character)}, {@link #hasOption(String)}, or
	 *             {@link #countCollisions(Option)}
	 */
	public OptionGroup add(OptionWrapper option) throws DuplicateOptionException;

	/**
	 * <p>
	 * Adds the given options to this option scheme, by iterating over the collection and invoking
	 * {@link #add(Option)} on each non-{@code null} element. If the
	 * {@link DuplicateOptionException} is raised, then all the incoming options will have added
	 * correctly up to the one first one that generated a conflict.
	 * </p>
	 *
	 * @param options
	 *            the options to add
	 * @throws DuplicateOptionException
	 */
	public <O extends Option> OptionGroup add(Collection<O> options) throws DuplicateOptionException;

	/**
	 * Remove any and all options (a maximum of 2) that may collide with the given option's short or
	 * long option forms. If {@code null} is returned, then there was no collision.
	 *
	 * @param option
	 *            the option to check against
	 * @return the options that were removed
	 */
	public Collection<Option> remove(Option option);

	/**
	 * See {@link #remove(Option)}.
	 *
	 * @param option
	 *            the option to check against
	 * @return the options that were removed
	 */
	public Collection<Option> remove(OptionWrapper option);

	/**
	 * Remove the option which matches the given long option
	 *
	 * @param longOpt
	 *            the long option
	 * @return the option which matches the given long option, or {@code null} if none matches.
	 */
	public Option remove(String longOpt);

	/**
	 * Remove the option which matches the given short option
	 *
	 * @param shortOpt
	 *            the short option
	 * @return the option which matches the given short option, or {@code null} if none matches.
	 */
	public Option remove(Character shortOpt);

	/**
	 * Returns the option scheme that this group is associated to, if any. If this group is not
	 * associated to any scheme, {@code null} is returned.
	 *
	 * @return the option scheme that this group is associated to, if any
	 */
	public OptionScheme getScheme();
}