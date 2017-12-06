package com.armedia.caliente.engine.ucm.model;

public class UcmRuntimeServiceException extends UcmRuntimeException {
	private static final long serialVersionUID = 1L;

	public UcmRuntimeServiceException() {
	}

	public UcmRuntimeServiceException(String message) {
		super(message);
	}

	public UcmRuntimeServiceException(Throwable cause) {
		super(cause);
	}

	public UcmRuntimeServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmRuntimeServiceException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}