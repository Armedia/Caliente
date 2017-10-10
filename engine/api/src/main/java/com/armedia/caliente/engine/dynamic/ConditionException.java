package com.armedia.caliente.engine.dynamic;

import com.armedia.caliente.engine.TransferEngineException;

public class ConditionException extends TransferEngineException {
	private static final long serialVersionUID = 1L;

	public ConditionException() {
	}

	public ConditionException(String message) {
		super(message);
	}

	public ConditionException(Throwable cause) {
		super(cause);
	}

	public ConditionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConditionException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}