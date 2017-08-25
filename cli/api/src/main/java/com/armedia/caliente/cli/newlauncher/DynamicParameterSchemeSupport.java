package com.armedia.caliente.cli.newlauncher;

import com.armedia.caliente.cli.ParameterScheme;
import com.armedia.caliente.cli.ParameterValues;
import com.armedia.caliente.cli.token.Token;

public interface DynamicParameterSchemeSupport {

	/**
	 * <p>
	 * Extend {@code scheme} by adding any parameters that may be supported additionally from the
	 * base scheme. The following restrictions apply:
	 * </p>
	 * <ul>
	 * <li>Parameters may not be removed</li>
	 * <li>New parameters may not collide with existing ones with either short or long options. This
	 * will result in an exception being raised and parsing aborted</li>
	 * </ul>
	 *
	 * @param currentNumber
	 *            The number of times the method has been invoked during the parsing
	 * @param baseValues
	 *            the values captured so far for the base parameters (immutable)
	 * @param currentCommand
	 *            the name of the command currently being processed, if any ({@code null} if none)
	 * @param commandValues
	 *            the values captured so far for the command's parameters (immutable, {@code null}
	 *            if no command is active)
	 * @param scheme
	 *            the scheme to enhance via calls to
	 *            {@link ParameterScheme#addParameter(com.armedia.caliente.cli.Parameter)} or
	 *            {@link ParameterScheme#addParameters(com.armedia.caliente.cli.Parameter...)}
	 */
	public void extendDynamicScheme(int currentNumber, ParameterValues baseValues, String currentCommand,
		ParameterValues commandValues, Token currentToken, ExtensibleParameterScheme scheme);

}