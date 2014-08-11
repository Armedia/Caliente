package com.delta.cmsmf.exception;

/**
 * The Class CMSMFException. This is a CMSMF application exception.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new CMSMF exception.
	 */
	public CMSMFException() {
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
	public CMSMFException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new CMSMF exception.
	 * 
	 * @param message
	 *            the exception message
	 */
	public CMSMFException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new CMSMF exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public CMSMFException(Throwable cause) {
		super(cause);
	}

}
