package com.armedia.caliente.engine.dynamic.filter;

public class ObjectSkippedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ObjectSkippedException() {
	}

	public ObjectSkippedException(String message) {
		super(message);
	}

	public ObjectSkippedException(Throwable cause) {
		super(cause);
	}

	public ObjectSkippedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectSkippedException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}