package com.delta.cmsmf.exception;

import java.io.IOException;

/**
 * The Class CMSMFException. This is a CMSMF application exception thrown
 * to suggest that I/O error of some sort has occurred.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFIOException extends IOException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new cMSMFIO exception.
	 */
	public CMSMFIOException() {
	}

	/**
	 * Instantiates a new cMSMFIO exception.
	 *
	 * @param arg0
	 *            the arg0
	 * @param arg1
	 *            the arg1
	 */
	public CMSMFIOException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * Instantiates a new cMSMFIO exception.
	 *
	 * @param arg0
	 *            the arg0
	 */
	public CMSMFIOException(String arg0) {
		super(arg0);
	}

	/**
	 * Instantiates a new cMSMFIO exception.
	 *
	 * @param arg0
	 *            the arg0
	 */
	public CMSMFIOException(Throwable arg0) {
		super(arg0);
	}

}
