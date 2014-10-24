/**
 *
 */

package com.armedia.cmf.exporter;

/**
 * @author diego
 *
 */
public class CmsExportException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmsExportException() {
		super();
	}

	public CmsExportException(String message, Throwable cause) {
		super(message, cause);
	}

	public CmsExportException(String message) {
		super(message);
	}

	public CmsExportException(Throwable cause) {
		super(cause);
	}
}
