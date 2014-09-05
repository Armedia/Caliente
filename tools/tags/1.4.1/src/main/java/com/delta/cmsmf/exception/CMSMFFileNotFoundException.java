package com.delta.cmsmf.exception;

import java.io.FileNotFoundException;

/**
 * The Class CMSMFException. This is a CMSMF application exception thrown
 * to suggest that the file being opened does not exist.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFFileNotFoundException extends FileNotFoundException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new CMSMF file not found exception.
	 */
	public CMSMFFileNotFoundException() {
	}

	/**
	 * Instantiates a new CMSMF file not found exception.
	 *
	 * @param message
	 *            the message
	 */
	public CMSMFFileNotFoundException(String message) {
		super(message);
	}

}
