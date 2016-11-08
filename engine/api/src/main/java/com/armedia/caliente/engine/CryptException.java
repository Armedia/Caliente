/**
 * *******************************************************************
 *
 * THIS SOFTWARE IS PROTECTED BY U.S. AND INTERNATIONAL COPYRIGHT LAWS. REPRODUCTION OF ANY PORTION
 * OF THE SOURCE CODE, CONTAINED HEREIN, OR ANY PORTION OF THE PRODUCT, EITHER IN PART OR WHOLE, IS
 * STRICTLY PROHIBITED.
 *
 * Confidential Property of Armedia LLC. (c) Copyright Armedia LLC 2011. All Rights reserved.
 *
 * *******************************************************************
 */
package com.armedia.caliente.engine;

/**
 * @author drivera@armedia.com
 *
 */
public class CryptException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * @param message
	 */
	CryptException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	CryptException(String message, Throwable cause) {
		super(message, cause);
	}
}