package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 */
public class CmsStorageException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmsStorageException() {
		super();
	}

	public CmsStorageException(String message, Throwable cause) {
		super(message, cause);
	}

	public CmsStorageException(String message) {
		super(message);
	}

	public CmsStorageException(Throwable cause) {
		super(cause);
	}
}