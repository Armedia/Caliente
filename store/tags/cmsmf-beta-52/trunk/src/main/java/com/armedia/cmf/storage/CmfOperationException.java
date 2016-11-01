package com.armedia.cmf.storage;

public class CmfOperationException extends CmfStorageException {
	private static final long serialVersionUID = 1L;

	public CmfOperationException() {
	}

	public CmfOperationException(String message, Throwable cause) {
		super(message, cause);
	}

	public CmfOperationException(String message) {
		super(message);
	}

	public CmfOperationException(Throwable cause) {
		super(cause);
	}
}
