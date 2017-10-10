package com.armedia.caliente.engine.dynamic;

import com.armedia.caliente.engine.TransferEngineException;

public class ActionException extends TransferEngineException {
	private static final long serialVersionUID = 1L;

	public ActionException() {
	}

	public ActionException(String message) {
		super(message);
	}

	public ActionException(Throwable cause) {
		super(cause);
	}

	public ActionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ActionException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}