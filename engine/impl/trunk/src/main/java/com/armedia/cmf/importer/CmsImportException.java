/**
 *
 */

package com.armedia.cmf.importer;

/**
 * @author diego
 *
 */
public class CmsImportException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmsImportException() {
		super();
	}

	public CmsImportException(String message, Throwable cause) {
		super(message, cause);
	}

	public CmsImportException(String message) {
		super(message);
	}

	public CmsImportException(Throwable cause) {
		super(cause);
	}
}
