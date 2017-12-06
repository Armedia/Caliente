/**
 *
 */

package com.armedia.caliente.engine.importer;

import com.armedia.caliente.engine.TransferEngineException;

/**
 * @author diego
 *
 */
public class ImportException extends TransferEngineException {
	private static final long serialVersionUID = 1L;

	public ImportException() {
		super();
	}

	public ImportException(String message, Throwable cause) {
		super(message, cause);
	}

	public ImportException(String message) {
		super(message);
	}

	public ImportException(Throwable cause) {
		super(cause);
	}

	public ImportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}