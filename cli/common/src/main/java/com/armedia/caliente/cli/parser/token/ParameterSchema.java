package com.armedia.caliente.cli.parser.token;

import java.util.Set;

public interface ParameterSchema extends ParameterSubSchema {

	/**
	 * <p>
	 * Returns {@code true} if this schema requires a SubSet as its first positional parameter,
	 * {@code false} otherwise.
	 * </p>
	 * 
	 * @return {@code true} if this schema requires a SubSet as its first positional parameter,
	 *         {@code false} otherwise.
	 *         </p>
	 */
	public boolean isRequiresSubSet();

	/**
	 * <p>
	 * Returns a {@link ParameterSubSchema} instance that includes specific sub-commands and parameters to
	 * be treated distinctly from the parent's.
	 * </p>
	 *
	 * @param subName
	 *            name or alias of the subset to find
	 * @return the named instance, or {@code null} if none exists
	 */
	public ParameterSubSchema getSubSet(String subName);

	/**
	 * <p>
	 * Check if a given subset is defined in this set
	 * </p>
	 *
	 * @param subName
	 *            name or alias of the subset to find
	 * @return {@code true} if the subset exists, false otherwise
	 */
	public boolean hasSubSet(String subName);

	/**
	 * <p>
	 * Returns the set of names of subsets that are available for this parameter set
	 * </p>
	 *
	 * @return the set of names of subsets that are available for this parameter set
	 */
	public Set<String> getSubSetNames();

	/**
	 * <p>
	 * Returns the definitive, non-aliased name for the given subset resolving any aliasing that may
	 * be necessary
	 * </p>
	 *
	 * @param alias
	 * @return the real, primary name for the given subset
	 */
	public String getSubSetName(String alias);

	/**
	 * <p>
	 * Returns the aliases, if any, that the given subset can be known as
	 * </p>
	 *
	 * @param subName
	 * @return the aliases, if any, that the given subset can be known as - may return {@code null}
	 */
	public Set<String> getSubSetAliases(String subName);

}