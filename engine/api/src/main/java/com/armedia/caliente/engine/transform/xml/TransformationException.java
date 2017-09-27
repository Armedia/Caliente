package com.armedia.caliente.engine.transform.xml;

public class TransformationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TransformationException() {
	}

	public TransformationException(String message) {
		super(message);
	}

	public TransformationException(Throwable cause) {
		super(cause);
	}

	public TransformationException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransformationException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}