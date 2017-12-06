/**
 *
 */

package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.TransferEngineException;

/**
 * @author diego
 *
 */
public class ExportException extends TransferEngineException {
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

	public ExportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}