package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 */
public class DuplicateCmfStoreException extends Exception {
	private static final long serialVersionUID = 1L;

	public DuplicateCmfStoreException() {
		super();
	}

	public DuplicateCmfStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateCmfStoreException(String message) {
		super(message);
	}

	public DuplicateCmfStoreException(Throwable cause) {
		super(cause);
	}
}