package com.armedia.caliente.cli;

import com.armedia.caliente.cli.exception.DuplicateOptionException;
import com.armedia.caliente.cli.exception.DuplicateOptionGroupException;

public interface OptionSchemeExtender {

	/**
	 * Adds a group to the currently active option scheme. If a group with the same name already
	 * exists, a {@link DuplicateOptionGroupException} will be raised. If any of the groups options
	 * conflict with the active scheme's options, a {@link DuplicateOptionException} will be raised.
	 * If the given option group is empty (i.e. contains no options) or {@code null}, this method
	 * does nothing.
	 *
	 * @param group
	 *            the group to add
	 * @return a reference to this object
	 * @throws DuplicateOptionGroupException
	 *             if another group with the same name is already defined
	 * @throws DuplicateOptionException
	 *             if one of the group's options conflicts with an already-defined option
	 */
	public OptionSchemeExtender addGroup(OptionGroup group);

	/**
	 * Check to see if the short option is already defined, to help avoid
	 * {@link DuplicateOptionException} when invoking {@link #addGroup(OptionGroup)}. If
	 * {@code option} is {@code null}, this method returns {@code false}.
	 *
	 * @param option
	 *            the option to check for
	 * @return {@code true} if the option is not {@code null} and is already defined, {@code false}
	 *         otherwise
	 */
	public boolean hasOption(Character option);

	/**
	 * Check to see if the long option is already defined, to help avoid
	 * {@link DuplicateOptionException} when invoking {@link #addGroup(OptionGroup)}. If
	 * {@code option} is {@code null}, this method returns {@code false}.
	 *
	 * @param option
	 *            the option to check for
	 * @return {@code true} if the option is not {@code null} and is already defined, {@code false}
	 *         otherwise
	 */
	public boolean hasOption(String option);

	/**
	 * Check to see if either the short or long options (or both) from the given option object are
	 * already defined, to avoid {@link DuplicateOptionException} when invoking
	 * {@link #addGroup(OptionGroup)}. If {@code option} is {@code null}, this method returns
	 * {@code false}.
	 *
	 * @param option
	 *            the option to check for
	 * @return {@code 0} if {@code option} is {@code null} or there is no matching option, {@code 1}
	 *         if only the short option matched, {@code 2} if only the long option matched, or
	 *         {@code 3} if both short and long options matched.
	 */
	public int hasOption(Option option);

	/**
	 * Check to see if a group with the given name is already defined, to avoid
	 * {@link DuplicateOptionGroupException} when invoking {@link #addGroup(OptionGroup)}. If
	 * {@code name} is {@code null}, this method returns {@code false}.
	 *
	 * @param name
	 * @return {@code true} if {@code name} is not {@code null} and a group with the given name is
	 *         already defined, {@code false} otherwise
	 */
	public boolean hasGroup(String name);
}