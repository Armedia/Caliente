package com.armedia.caliente.engine.dynamic;

public class ProcessingCompletedException extends RuntimeDynamicElementException {
	private static final long serialVersionUID = 1L;

	public ProcessingCompletedException() {
	}

	public ProcessingCompletedException(String message) {
		super(message);
	}

	public ProcessingCompletedException(Throwable cause) {
		super(cause);
	}

	public ProcessingCompletedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProcessingCompletedException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}