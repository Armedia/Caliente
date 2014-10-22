package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 */
public class CmsInvalidObjectStoreClassException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmsInvalidObjectStoreClassException() {
		super();
	}

	public CmsInvalidObjectStoreClassException(String message, Throwable cause) {
		super(message, cause);
	}

	public CmsInvalidObjectStoreClassException(String message) {
		super(message);
	}

	public CmsInvalidObjectStoreClassException(Throwable cause) {
		super(cause);
	}
}