package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 */
public class DuplicateObjectStoreException extends Exception {
	private static final long serialVersionUID = 1L;

	public DuplicateObjectStoreException() {
		super();
	}

	public DuplicateObjectStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateObjectStoreException(String message) {
		super(message);
	}

	public DuplicateObjectStoreException(Throwable cause) {
		super(cause);
	}
}