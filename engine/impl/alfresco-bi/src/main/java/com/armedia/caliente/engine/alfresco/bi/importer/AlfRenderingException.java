package com.armedia.caliente.engine.alfresco.bi.importer;

public class AlfRenderingException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AlfRenderingException() {
	}

	public AlfRenderingException(String message) {
		super(message);
	}

	public AlfRenderingException(Throwable cause) {
		super(cause);
	}

	public AlfRenderingException(String message, Throwable cause) {
		super(message, cause);
	}

	public AlfRenderingException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}