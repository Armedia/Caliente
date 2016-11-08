/**
 *
 */

package com.armedia.caliente.engine.documentum;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class UnsupportedObjectClassException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	UnsupportedObjectClassException(Class<?> klass) {
		super(String.format("The object class [%s] is not supported", klass.getCanonicalName()));
	}
}