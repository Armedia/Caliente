/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

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

	public ShptSessionException(ServiceException cause) {
		super(cause);
	}

	public ShptSessionException(String message, ServiceException cause) {
		super(message, cause);
	}

	public ServiceException getServiceException() {
		return ServiceException.class.cast(getCause());
	}
}