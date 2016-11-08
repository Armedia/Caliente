/**
 *
 */

package com.armedia.caliente.engine;

/**
 * @author diego
 *
 */
public class TransferEngineException extends Exception {
	private static final long serialVersionUID = 1L;

	public TransferEngineException() {
		super();
	}

	public TransferEngineException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransferEngineException(String message) {
		super(message);
	}

	public TransferEngineException(Throwable cause) {
		super(cause);
	}
}