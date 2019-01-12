package com.armedia.caliente.tools;

public class ResourceLoaderException extends Exception {
	private static final long serialVersionUID = 1L;

	public ResourceLoaderException() {
	}

	public ResourceLoaderException(String message) {
		super(message);
	}

	public ResourceLoaderException(Throwable cause) {
		super(cause);
	}

	public ResourceLoaderException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceLoaderException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
