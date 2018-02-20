package com.armedia.caliente.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OptionValueFilter {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public final boolean isAllowed(String value) {
		return ((value != null) && checkValue(value));
	}

	protected abstract boolean checkValue(String value);

	public abstract String getDefinition();
}