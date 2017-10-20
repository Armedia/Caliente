package com.armedia.caliente.engine.dynamic.filter;

import com.armedia.caliente.engine.dynamic.DynamicElementException;

public class ObjectFilterException extends DynamicElementException {
	private static final long serialVersionUID = 1L;

	public ObjectFilterException() {
	}

	public ObjectFilterException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectFilterException(String message) {
		super(message);
	}

	public ObjectFilterException(Throwable cause) {
		super(cause);
	}

	public ObjectFilterException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}