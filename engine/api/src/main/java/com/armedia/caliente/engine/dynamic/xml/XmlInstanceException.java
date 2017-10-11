package com.armedia.caliente.engine.dynamic.xml;

import com.armedia.caliente.engine.dynamic.DynamicElementException;

public class XmlInstanceException extends DynamicElementException {
	private static final long serialVersionUID = 1L;

	public XmlInstanceException() {
	}

	public XmlInstanceException(String message) {
		super(message);
	}

	public XmlInstanceException(Throwable cause) {
		super(cause);
	}

	public XmlInstanceException(String message, Throwable cause) {
		super(message, cause);
	}

	public XmlInstanceException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}