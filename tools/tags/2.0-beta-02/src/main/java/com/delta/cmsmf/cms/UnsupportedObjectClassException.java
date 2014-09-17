/**
 *
 */

package com.delta.cmsmf.cms;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class UnsupportedObjectClassException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	UnsupportedObjectClassException(Class<?> klass) {
		super(String.format("The object class [%s] is not supported", klass.getCanonicalName()));
	}
}