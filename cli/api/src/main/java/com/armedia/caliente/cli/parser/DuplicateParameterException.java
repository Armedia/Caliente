package com.armedia.caliente.cli.parser;

import com.armedia.caliente.cli.CommandLineException;
import com.armedia.caliente.cli.Parameter;

public class DuplicateParameterException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final Parameter existing;
	private final Parameter incoming;

	public DuplicateParameterException(String msg, Parameter existing,
		Parameter incoming) {
		super(msg);
		if (existing == null) { throw new IllegalArgumentException("Must have an existing value"); }
		if (incoming == null) { throw new IllegalArgumentException("Must have an incoming value"); }
		if (incoming == existing) { throw new IllegalArgumentException(
			"The existing and incoming definitions must be different"); }
		this.existing = existing;
		this.incoming = incoming;
	}

	public Parameter getExisting() {
		return this.existing;
	}

	public Parameter getIncoming() {
		return this.incoming;
	}
}