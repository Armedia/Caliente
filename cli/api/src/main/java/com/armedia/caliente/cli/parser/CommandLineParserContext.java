package com.armedia.caliente.cli.parser;

import java.util.Collection;
import java.util.Collections;

public class CommandLineParserContext {
	protected final CommandLine cl;

	protected CommandLineParserContext(CommandLine cl) {
		if (cl == null) { throw new IllegalArgumentException("Must provide a CommandLine instance"); }
		this.cl = cl;
	}

	public void setParameter(Parameter p) {
		setParameter(p, null);
	}

	public void setParameter(Parameter p, Collection<String> values) {
		if (p == null) { throw new IllegalArgumentException("Must provide a parameter to set"); }
		if (values == null) {
			values = Collections.emptyList();
		}
		this.cl.setParameterValues(p, values);
	}

	public void addRemainingParameters(Collection<String> remaining) {
		if (remaining == null) {
			remaining = Collections.emptyList();
		}
		this.cl.addRemainingParameters(remaining);
	}
}