package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 */
public class DuplicateStoreException extends Exception {
	private static final long serialVersionUID = 1L;

	public DuplicateStoreException() {
		super();
	}

	public DuplicateStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateStoreException(String message) {
		super(message);
	}

	public DuplicateStoreException(Throwable cause) {
		super(cause);
	}
}