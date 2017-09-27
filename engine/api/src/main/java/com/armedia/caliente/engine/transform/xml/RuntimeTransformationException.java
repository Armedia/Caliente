package com.armedia.caliente.engine.transform.xml;

public class RuntimeTransformationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public RuntimeTransformationException() {
	}

	public RuntimeTransformationException(String message) {
		super(message);
	}

	public RuntimeTransformationException(Throwable cause) {
		super(cause);
	}

	public RuntimeTransformationException(String message, Throwable cause) {
		super(message, cause);
	}

	public RuntimeTransformationException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}