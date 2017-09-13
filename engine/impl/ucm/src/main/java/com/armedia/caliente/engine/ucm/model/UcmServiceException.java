package com.armedia.caliente.engine.ucm.model;

public class UcmServiceException extends UcmException {
	private static final long serialVersionUID = 1L;

	public UcmServiceException() {
	}

	public UcmServiceException(String message) {
		super(message);
	}

	public UcmServiceException(Throwable cause) {
		super(cause);
	}

	public UcmServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}