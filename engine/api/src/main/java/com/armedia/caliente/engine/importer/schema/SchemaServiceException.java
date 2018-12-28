package com.armedia.caliente.engine.importer.schema;

public class SchemaServiceException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SchemaServiceException() {
	}

	public SchemaServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public SchemaServiceException(String message) {
		super(message);
	}

	public SchemaServiceException(Throwable cause) {
		super(cause);
	}

	public SchemaServiceException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}