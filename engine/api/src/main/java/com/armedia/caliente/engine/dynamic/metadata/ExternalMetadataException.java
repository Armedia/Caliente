package com.armedia.caliente.engine.dynamic.metadata;

import com.armedia.caliente.engine.dynamic.DynamicElementException;

public class ExternalMetadataException extends DynamicElementException {
	private static final long serialVersionUID = 1L;

	public ExternalMetadataException() {
	}

	public ExternalMetadataException(String message) {
		super(message);
	}

	public ExternalMetadataException(Throwable cause) {
		super(cause);
	}

	public ExternalMetadataException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExternalMetadataException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}