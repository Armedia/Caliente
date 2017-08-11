package com.armedia.caliente.engine.dfc.importer;

import com.armedia.caliente.engine.importer.ImportException;

public class MultipleUserMatchesException extends ImportException {
	private static final long serialVersionUID = 1L;

	public MultipleUserMatchesException() {
		super();
	}

	public MultipleUserMatchesException(String message, Throwable cause) {
		super(message, cause);
	}

	public MultipleUserMatchesException(String message) {
		super(message);
	}

	public MultipleUserMatchesException(Throwable cause) {
		super(cause);
	}
}