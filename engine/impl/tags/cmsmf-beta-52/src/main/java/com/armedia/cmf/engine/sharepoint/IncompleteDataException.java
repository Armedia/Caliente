package com.armedia.cmf.engine.sharepoint;

public class IncompleteDataException extends Exception {
	private static final long serialVersionUID = 1L;

	public IncompleteDataException() {
	}

	public IncompleteDataException(String message) {
		super(message);
	}

	public IncompleteDataException(Throwable cause) {
		super(cause);
	}

	public IncompleteDataException(String message, Throwable cause) {
		super(message, cause);
	}

}