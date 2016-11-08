/**
 *
 */

package com.armedia.caliente.engine.sharepoint;

/**
 * @author diego
 *
 */
public class ShptException extends Exception {
	private static final long serialVersionUID = 1L;

	public ShptException() {
	}

	public ShptException(String message) {
		super(message);
	}

	public ShptException(Throwable cause) {
		super(cause);
	}

	public ShptException(String message, Throwable cause) {
		super(message, cause);
	}
}