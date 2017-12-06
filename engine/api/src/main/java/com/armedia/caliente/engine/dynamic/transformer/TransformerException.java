package com.armedia.caliente.engine.dynamic.transformer;

import com.armedia.caliente.engine.dynamic.DynamicElementException;

public class TransformerException extends DynamicElementException {
	private static final long serialVersionUID = 1L;

	public TransformerException() {
	}

	public TransformerException(String message) {
		super(message);
	}

	public TransformerException(Throwable cause) {
		super(cause);
	}

	public TransformerException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransformerException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}