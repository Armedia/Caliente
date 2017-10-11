package com.armedia.caliente.engine.filter;

import com.armedia.caliente.engine.dynamic.DynamicElementException;

public class FilterException extends DynamicElementException {
	private static final long serialVersionUID = 1L;

	public FilterException() {
	}

	public FilterException(String message, Throwable cause) {
		super(message, cause);
	}

	public FilterException(String message) {
		super(message);
	}

	public FilterException(Throwable cause) {
		super(cause);
	}

	public FilterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}