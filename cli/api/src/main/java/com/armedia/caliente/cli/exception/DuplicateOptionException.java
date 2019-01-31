package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Option;

public class DuplicateOptionException extends CommandLineRuntimeException {
	private static final long serialVersionUID = 1L;

	private final Option existing;
	private final Option incoming;

	public DuplicateOptionException(String msg, Option existing, Option incoming) {
		super(msg);
		if (existing == null) { throw new IllegalArgumentException("Must have an existing value"); }
		if (incoming == null) { throw new IllegalArgumentException("Must have an incoming value"); }
		if (incoming == existing) {
			throw new IllegalArgumentException("The existing and incoming definitions must be different");
		}
		this.existing = existing;
		this.incoming = incoming;
	}

	public Option getExisting() {
		return this.existing;
	}

	public Option getIncoming() {
		return this.incoming;
	}
}