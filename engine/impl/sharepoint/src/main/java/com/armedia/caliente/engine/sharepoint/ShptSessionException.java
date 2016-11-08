/**
 *
 */

package com.armedia.caliente.engine.sharepoint;

import com.independentsoft.share.ServiceException;

/**
 * @author diego
 *
 */
public class ShptSessionException extends Exception {
	private static final long serialVersionUID = 1L;

	public ShptSessionException() {
	}

	public ShptSessionException(String message) {
		super(message);
	}

	public ShptSessionException(Throwable cause) {
		super(cause);
	}

	public ShptSessionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceException getServiceException() {
		Throwable cause = getCause();
		if ((cause != null) && (cause instanceof ServiceException)) { return ServiceException.class.cast(cause); }
		return null;
	}
}