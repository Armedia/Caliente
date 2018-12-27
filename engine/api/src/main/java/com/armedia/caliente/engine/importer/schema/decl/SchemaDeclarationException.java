package com.armedia.caliente.engine.importer.schema.decl;

import com.armedia.caliente.engine.importer.ImportException;

public class SchemaDeclarationException extends ImportException {
	private static final long serialVersionUID = 1L;

	public SchemaDeclarationException() {
	}

	public SchemaDeclarationException(String message, Throwable cause) {
		super(message, cause);
	}

	public SchemaDeclarationException(String message) {
		super(message);
	}

	public SchemaDeclarationException(Throwable cause) {
		super(cause);
	}

	public SchemaDeclarationException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}