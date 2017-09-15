package com.armedia.caliente.engine.ucm.model;

public class UcmRevisionRevisionNotFoundException extends UcmException {
	private static final long serialVersionUID = 1L;

	public UcmRevisionRevisionNotFoundException() {
	}

	public UcmRevisionRevisionNotFoundException(String message) {
		super(message);
	}

	public UcmRevisionRevisionNotFoundException(Throwable cause) {
		super(cause);
	}

	public UcmRevisionRevisionNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmRevisionRevisionNotFoundException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}