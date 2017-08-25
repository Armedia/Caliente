package com.armedia.caliente.cli;

/**
 * <p>
 * This is a utility interface that helps in the use of classes (like {@link Enum Enums}, for
 * instance) that can be used as a {@link Parameter} analog because they contain a single parameter,
 * and thus reduce the amount of code written to support a simple coding strategy.
 * </p>
 *
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public interface ParameterWrapper {

	/**
	 * <p>
	 * Return the parameter that this wrapper is keeping
	 * </p>
	 *
	 * @return the parameter
	 */
	public Parameter getParameter();

}