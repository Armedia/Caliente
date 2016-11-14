package com.armedia.caliente.cli.caliente.exception;

/**
 * The Class CalienteException. This is a CMSMF application exception.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CalienteException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new CMSMF exception.
	 */
	public CalienteException() {
		super();
	}

	/**
	 * Instantiates a new cMSMF exception.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public CalienteException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new CMSMF exception.
	 *
	 * @param message
	 *            the exception message
	 */
	public CalienteException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new CMSMF exception.
	 *
	 * @param cause
	 *            the cause
	 */
	public CalienteException(Throwable cause) {
		super(cause);
	}

}
