package com.armedia.caliente.engine.dynamic;

import com.armedia.caliente.engine.TransferException;

public class DynamicElementException extends TransferException {
	private static final long serialVersionUID = 1L;

	public DynamicElementException() {
	}

	public DynamicElementException(String message, Throwable cause) {
		super(message, cause);
	}

	public DynamicElementException(String message) {
		super(message);
	}

	public DynamicElementException(Throwable cause) {
		super(cause);
	}

	public DynamicElementException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}