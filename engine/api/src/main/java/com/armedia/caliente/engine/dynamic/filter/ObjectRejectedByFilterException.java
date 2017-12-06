package com.armedia.caliente.engine.dynamic.filter;

import com.armedia.caliente.engine.dynamic.RuntimeDynamicElementException;

public class ObjectRejectedByFilterException extends RuntimeDynamicElementException {
	private static final long serialVersionUID = 1L;

	public ObjectRejectedByFilterException() {
	}

	public ObjectRejectedByFilterException(String message) {
		super(message);
	}

	public ObjectRejectedByFilterException(Throwable cause) {
		super(cause);
	}

	public ObjectRejectedByFilterException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectRejectedByFilterException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}