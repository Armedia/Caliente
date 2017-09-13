package com.armedia.caliente.engine.ucm.model;

public class UcmException extends Exception {
	private static final long serialVersionUID = 1L;

	public UcmException() {
	}

	public UcmException(String message) {
		super(message);
	}

	public UcmException(Throwable cause) {
		super(cause);
	}

	public UcmException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}