package com.armedia.caliente.engine.dynamic.transformer;

public class TransformationCompletedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TransformationCompletedException() {
	}

	public TransformationCompletedException(String message) {
		super(message);
	}

	public TransformationCompletedException(Throwable cause) {
		super(cause);
	}

	public TransformationCompletedException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransformationCompletedException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}