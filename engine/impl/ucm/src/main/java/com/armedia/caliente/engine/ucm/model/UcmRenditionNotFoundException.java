package com.armedia.caliente.engine.ucm.model;

public class UcmRenditionNotFoundException extends UcmException {
	private static final long serialVersionUID = 1L;

	public UcmRenditionNotFoundException() {
	}

	public UcmRenditionNotFoundException(String message) {
		super(message);
	}

	public UcmRenditionNotFoundException(Throwable cause) {
		super(cause);
	}

	public UcmRenditionNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmRenditionNotFoundException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}