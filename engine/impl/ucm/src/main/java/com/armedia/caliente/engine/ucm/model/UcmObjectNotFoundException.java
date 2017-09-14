package com.armedia.caliente.engine.ucm.model;

public class UcmObjectNotFoundException extends UcmException {
	private static final long serialVersionUID = 1L;

	public UcmObjectNotFoundException() {
	}

	public UcmObjectNotFoundException(String message) {
		super(message);
	}

	public UcmObjectNotFoundException(Throwable cause) {
		super(cause);
	}

	public UcmObjectNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmObjectNotFoundException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}