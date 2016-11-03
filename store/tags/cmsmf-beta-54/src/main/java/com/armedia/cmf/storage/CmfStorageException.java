package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 */
public class CmfStorageException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmfStorageException() {
		super();
	}

	public CmfStorageException(String message, Throwable cause) {
		super(message, cause);
	}

	public CmfStorageException(String message) {
		super(message);
	}

	public CmfStorageException(Throwable cause) {
		super(cause);
	}
}