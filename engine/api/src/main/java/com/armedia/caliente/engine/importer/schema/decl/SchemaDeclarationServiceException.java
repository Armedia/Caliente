package com.armedia.caliente.engine.importer.schema.decl;

import com.armedia.caliente.engine.importer.ImportException;

public class SchemaDeclarationServiceException extends ImportException {
	private static final long serialVersionUID = 1L;

	public SchemaDeclarationServiceException() {
	}

	public SchemaDeclarationServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public SchemaDeclarationServiceException(String message) {
		super(message);
	}

	public SchemaDeclarationServiceException(Throwable cause) {
		super(cause);
	}

	public SchemaDeclarationServiceException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}