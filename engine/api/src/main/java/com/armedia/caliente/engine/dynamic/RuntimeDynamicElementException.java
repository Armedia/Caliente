package com.armedia.caliente.engine.dynamic;

public class RuntimeDynamicElementException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public RuntimeDynamicElementException() {
	}

	public RuntimeDynamicElementException(String message) {
		super(message);
	}

	public RuntimeDynamicElementException(Throwable cause) {
		super(cause);
	}

	public RuntimeDynamicElementException(String message, Throwable cause) {
		super(message, cause);
	}

	public RuntimeDynamicElementException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}