package com.armedia.caliente.engine.dynamic.xml;

import com.armedia.caliente.engine.dynamic.DynamicElementException;

public class XmlNotFoundException extends DynamicElementException {
	private static final long serialVersionUID = 1L;

	public XmlNotFoundException() {
	}

	public XmlNotFoundException(String message) {
		super(message);
	}

	public XmlNotFoundException(Throwable cause) {
		super(cause);
	}

	public XmlNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public XmlNotFoundException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}