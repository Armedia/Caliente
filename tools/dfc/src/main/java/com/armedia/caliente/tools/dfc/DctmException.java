/**
 *
 */

package com.armedia.caliente.tools.dfc;

/**
 * @author diego
 *
 */
public class DctmException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	protected DctmException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	protected DctmException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	protected DctmException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	protected DctmException(Throwable cause) {
		super(cause);
	}
}