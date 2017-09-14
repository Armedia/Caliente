package com.armedia.caliente.engine.ucm.model;

public class UcmFileNotFoundException extends UcmObjectNotFoundException {
	private static final long serialVersionUID = 1L;

	public UcmFileNotFoundException() {
	}

	public UcmFileNotFoundException(String message) {
		super(message);
	}

	public UcmFileNotFoundException(Throwable cause) {
		super(cause);
	}

	public UcmFileNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmFileNotFoundException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}