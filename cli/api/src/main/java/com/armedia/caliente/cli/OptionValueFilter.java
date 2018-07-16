package com.armedia.caliente.cli;

public abstract class OptionValueFilter {

	public final boolean isAllowed(String value) {
		return ((value != null) && checkValue(value));
	}

	protected abstract boolean checkValue(String value);

	public abstract String getDefinition();
}