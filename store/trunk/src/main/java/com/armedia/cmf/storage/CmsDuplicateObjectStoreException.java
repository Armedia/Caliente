package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 */
public class CmsDuplicateObjectStoreException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmsDuplicateObjectStoreException() {
		super();
	}

	public CmsDuplicateObjectStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public CmsDuplicateObjectStoreException(String message) {
		super(message);
	}

	public CmsDuplicateObjectStoreException(Throwable cause) {
		super(cause);
	}
}