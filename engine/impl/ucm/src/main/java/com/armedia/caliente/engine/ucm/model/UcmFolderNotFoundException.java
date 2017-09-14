package com.armedia.caliente.engine.ucm.model;

public class UcmFolderNotFoundException extends UcmObjectNotFoundException {
	private static final long serialVersionUID = 1L;

	public UcmFolderNotFoundException() {
	}

	public UcmFolderNotFoundException(String message) {
		super(message);
	}

	public UcmFolderNotFoundException(Throwable cause) {
		super(cause);
	}

	public UcmFolderNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public UcmFolderNotFoundException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}