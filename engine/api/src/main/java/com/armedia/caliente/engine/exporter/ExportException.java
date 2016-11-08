/**
 *
 */

package com.armedia.caliente.engine.exporter;

/**
 * @author diego
 *
 */
public class ExportException extends Exception {
	private static final long serialVersionUID = 1L;

	public ExportException() {
		super();
	}

	public ExportException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExportException(String message) {
		super(message);
	}

	public ExportException(Throwable cause) {
		super(cause);
	}
}
