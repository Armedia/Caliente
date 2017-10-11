package com.armedia.caliente.engine.dynamic.filter;

import com.armedia.caliente.engine.dynamic.RuntimeDynamicElementException;

public class ObjectFilteredException extends RuntimeDynamicElementException {
	private static final long serialVersionUID = 1L;

	public ObjectFilteredException() {
	}

	public ObjectFilteredException(String message) {
		super(message);
	}

	public ObjectFilteredException(Throwable cause) {
		super(cause);
	}

	public ObjectFilteredException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectFilteredException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}