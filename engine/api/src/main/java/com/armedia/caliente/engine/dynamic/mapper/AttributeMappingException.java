package com.armedia.caliente.engine.dynamic.mapper;

import com.armedia.caliente.engine.dynamic.DynamicElementException;

public class AttributeMappingException extends DynamicElementException {
	private static final long serialVersionUID = 1L;

	public AttributeMappingException() {
	}

	public AttributeMappingException(String message) {
		super(message);
	}

	public AttributeMappingException(Throwable cause) {
		super(cause);
	}

	public AttributeMappingException(String message, Throwable cause) {
		super(message, cause);
	}

	public AttributeMappingException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}