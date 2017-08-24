package com.armedia.caliente.cli.parser.token;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

import com.armedia.caliente.cli.ParameterDefinition;
import com.armedia.commons.utilities.Tools;

public interface ParameterSubSchema extends Cloneable {

	/**
	 * <p>
	 * A default comparator that sorts parameters based on both their short and long options
	 * alphabetically, prioritizing the short option over the long one.
	 * </p>
	 */
	public static final Comparator<ParameterDefinition> DEFAULT_COMPARATOR = new Comparator<ParameterDefinition>() {

		private String getValue(ParameterDefinition p) {
			Character s = p.getShortOpt();
			String l = p.getLongOpt();
			return (s != null ? s.toString() : l);
		}

		@Override
		public int compare(ParameterDefinition a, ParameterDefinition b) {
			if (a == b) { return 0; }
			if (a == null) { return -1; }
			if (b == null) { return 1; }
			final String A = getValue(a);
			final String B = getValue(b);
			return Tools.compare(A, B);
		}
	};

	/**
	 * <p>
	 * Returns a brief description of this parameter set
	 * </p>
	 *
	 * @return a brief description of this parameter set (may be {@code null})
	 */
	public String getDescription();

	/**
	 * <p>
	 * Returns {@code true} if the given short option has been defined, {@code false} otherwise
	 * </p>
	 *
	 * @param shortOpt
	 * @return {@code true} if the given short option has been defined, {@code false} otherwise
	 */
	public boolean hasParameter(char shortOpt);

	/**
	 * <p>
	 * Returns the {@link ParameterDefinition} instance which describes the short option, or {@code null} if
	 * it's not defined.
	 * </p>
	 *
	 * @param shortOpt
	 * @return the {@link ParameterDefinition} instance which describes the short option
	 */
	public ParameterDefinition getParameter(char shortOpt);

	/**
	 * <p>
	 * Returns the names of the short options available in this instance
	 * </p>
	 *
	 * @return the names of the short options available in this instance
	 */
	public Set<Character> getShortOptions();

	/**
	 * <p>
	 * Returns {@code true} if the given long option has been defined, {@code false} otherwise
	 * </p>
	 *
	 * @param longOpt
	 * @return {@code true} if the given long option has been defined, {@code false} otherwise
	 */
	public boolean hasParameter(String longOpt);

	/**
	 * <p>
	 * Returns the {@link ParameterDefinition} instance which describes the long option, or {@code null} if
	 * it's not defined.
	 * </p>
	 *
	 * @param longOpt
	 * @return the {@link ParameterDefinition} instance which describes the long option
	 */
	public ParameterDefinition getParameter(String longOpt);

	/**
	 * <p>
	 * Returns the names of the long options available in this instance
	 * </p>
	 *
	 * @return the names of the long options available in this instance
	 */
	public Set<String> getLongOptions();

	/**
	 * <p>
	 * Returns a sorted {@link Collection} of the defined parameters based on the given
	 * {@link Comparator} instance.
	 * </p>
	 *
	 * @return a sorted {@link Collection} of the defined parameters
	 */
	public Collection<ParameterDefinition> getParameters(Comparator<? super ParameterDefinition> comparator);
}