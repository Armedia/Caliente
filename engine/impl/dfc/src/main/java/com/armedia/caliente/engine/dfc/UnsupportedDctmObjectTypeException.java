/**
 *
 */

package com.armedia.caliente.engine.dfc;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class UnsupportedDctmObjectTypeException extends Exception {
	private static final long serialVersionUID = 1L;

	public UnsupportedDctmObjectTypeException(String type) {
		super(String.format("The object type [%s] is not supported", type));
	}
}