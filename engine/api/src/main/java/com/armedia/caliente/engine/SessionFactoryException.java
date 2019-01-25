package com.armedia.caliente.engine;

public class SessionFactoryException extends Exception {
	private static final long serialVersionUID = 1L;

	public SessionFactoryException() {
	}

	public SessionFactoryException(String message) {
		super(message);
	}

	public SessionFactoryException(Throwable cause) {
		super(cause);
	}

	public SessionFactoryException(String message, Throwable cause) {
		super(message, cause);
	}

	public SessionFactoryException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
