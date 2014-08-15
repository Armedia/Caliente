package com.delta.cmsmf.exception;

/**
 * The Class CMSMFException. This is a CMSMF application exception thrown
 * to suggest there is some fatal error condition has occurred. The application
 * will exit gracefully in the event this exception is thrown.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFFatalException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new CMSMF exception.
	 */
	public CMSMFFatalException() {
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
	public CMSMFFatalException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new CMSMF exception.
	 * 
	 * @param message
	 *            the exception message
	 */
	public CMSMFFatalException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new CMSMF exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public CMSMFFatalException(Throwable cause) {
		super(cause);
	}

}
