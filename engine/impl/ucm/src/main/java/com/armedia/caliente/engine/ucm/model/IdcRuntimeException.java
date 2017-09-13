package com.armedia.caliente.engine.ucm.model;

public class IdcRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public IdcRuntimeException() {
	}

	public IdcRuntimeException(String message) {
		super(message);
	}

	public IdcRuntimeException(Throwable cause) {
		super(cause);
	}

	public IdcRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public IdcRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}