package com.armedia.caliente.cli;

import com.armedia.caliente.cli.token.Token;

public interface OptionSchemeExtensionSupport {

	/**
	 * <p>
	 * Extend {@code scheme} by adding any options that may be supported additionally from the base
	 * scheme. New options may not collide with existing ones with either short or long options.
	 * This will result in an exception being raised and parsing aborted.
	 * </p>
	 *
	 * @param currentNumber
	 *            The number of times the method has been invoked during the parsing
	 * @param baseValues
	 *            the values captured so far for the base options (immutable)
	 * @param currentCommand
	 *            the name of the command currently being processed, if any ({@code null} if none)
	 * @param commandValues
	 *            the values captured so far for the command's options (immutable, {@code null} if
	 *            no command is active)
	 * @param scheme
	 *            the scheme to enhance via calls to
	 *            {@link OptionScheme#add(com.armedia.caliente.cli.Option)}
	 */
	public void extendScheme(int currentNumber, OptionValues baseValues, String currentCommand,
		OptionValues commandValues, Token currentToken, OptionSchemeExtension scheme);

}