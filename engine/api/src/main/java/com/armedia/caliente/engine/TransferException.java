/**
 *
 */

package com.armedia.caliente.engine;

/**
 * @author diego
 *
 */
public class TransferException extends Exception {
	private static final long serialVersionUID = 1L;

	public TransferException() {
		super();
	}

	public TransferException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransferException(String message) {
		super(message);
	}

	public TransferException(Throwable cause) {
		super(cause);
	}

	public TransferException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}