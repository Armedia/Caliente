package com.armedia.caliente.cli.parser;

import com.armedia.caliente.cli.CommandLineException;
import com.armedia.caliente.cli.ParameterDefinition;

public class DuplicateParameterException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final ParameterDefinition existing;
	private final ParameterDefinition incoming;

	public DuplicateParameterException(String msg, ParameterDefinition existing,
		ParameterDefinition incoming) {
		super(msg);
		if (existing == null) { throw new IllegalArgumentException("Must have an existing value"); }
		if (incoming == null) { throw new IllegalArgumentException("Must have an incoming value"); }
		if (incoming == existing) { throw new IllegalArgumentException(
			"The existing and incoming definitions must be different"); }
		this.existing = existing;
		this.incoming = incoming;
	}

	public ParameterDefinition getExisting() {
		return this.existing;
	}

	public ParameterDefinition getIncoming() {
		return this.incoming;
	}
}