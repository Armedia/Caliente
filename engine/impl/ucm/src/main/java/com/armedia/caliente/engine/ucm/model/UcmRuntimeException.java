package com.armedia.caliente.engine.ucm.model;

public class UcmRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UcmRuntimeException() {
	}

	public UcmRuntimeException(String message) {
		super(message);
	}

	public UcmRuntimeException(Throwable cause) {
		super(cause);
	}

	public UcmRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}