package com.armedia.caliente.cli.exception;

public class DuplicateOptionGroupException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final String name;

	public DuplicateOptionGroupException(String name) {
		super(String.format("A group with the name [%s] already exists", name));
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}