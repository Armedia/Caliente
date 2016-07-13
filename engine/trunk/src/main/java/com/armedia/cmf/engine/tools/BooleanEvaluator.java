package com.armedia.cmf.engine.tools;

import java.util.Map;
import java.util.regex.Pattern;

public class BooleanEvaluator implements BooleanExpression {

	private static final Pattern IDENTIFIER = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]+:)?([a-zA-Z_][a-zA-Z_0-9]+)");
	private static final Pattern OPERATORS = Pattern.compile("(\\&\\&|\\|\\||\\^\\^|!)");

	public BooleanEvaluator(String expression, Map<String, BooleanExpression> context) throws Exception {
		// First things first: parse the damn thing :)
		// split characters: * ^ + ! ( )

		// Start consuming

		// a) Find all the operators

	}

	@Override
	public boolean evaluate(BooleanContext c) {
		return false;
	}
}