package com.armedia.caliente.cli.token;

public enum TokenType {
	//
	/**
	 * A short option (i.e. -c, -x, etc.) its value is always one character long
	 */
	SHORT_OPTION,

	/**
	 * A long option (i.e. --long-option) - its value is always a string
	 */
	LONG_OPTION,

	/**
	 * A "plain" string - i.e. no prefix of any kind
	 */
	STRING,
	//
	;
}