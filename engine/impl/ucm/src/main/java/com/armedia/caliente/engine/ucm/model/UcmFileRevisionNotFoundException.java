package com.armedia.caliente.engine.ucm.model;

public class UcmFileRevisionNotFoundException extends UcmException {
	private static final long serialVersionUID = 1L;

	public UcmFileRevisionNotFoundException() {
	}

	public UcmFileRevisionNotFoundException(String message) {
		super(message);
	}

	public UcmFileRevisionNotFoundException(Throwable cause) {
		super(cause);
	}

	public UcmFileRevisionNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmFileRevisionNotFoundException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}