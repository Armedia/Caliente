/**
 *
 */

package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class UnsupportedObjectTypeException extends Exception {
	private static final long serialVersionUID = 1L;

	UnsupportedObjectTypeException(String type) {
		super(String.format("The object type [%s] is not supported", type));
	}
}