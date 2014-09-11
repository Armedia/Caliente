/**
 *
 */

package com.delta.cmsmf.cms;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class UnsupportedObjectTypeException extends Exception {
	private static final long serialVersionUID = 1L;

	public UnsupportedObjectTypeException(String type) {
		super(String.format("The object type [%s] is not supported", type));
	}
}