package com.armedia.caliente.engine.dynamic.xml;

import com.armedia.caliente.engine.dynamic.DynamicElementException;

public class ExpressionException extends DynamicElementException {
	private static final long serialVersionUID = 1L;

	public ExpressionException() {
	}

	public ExpressionException(String message) {
		super(message);
	}

	public ExpressionException(Throwable cause) {
		super(cause);
	}

	public ExpressionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExpressionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}