package com.armedia.caliente.engine.dynamic.jaxb;

import com.armedia.caliente.engine.TransferEngineException;

public class ExpressionException extends TransferEngineException {
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