/**
 *
 */

package com.armedia.caliente.store;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class UnsupportedCmfTypeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	UnsupportedCmfTypeException(String type) {
		super(String.format("The object type [%s] is not supported", type));
	}

	public UnsupportedCmfTypeException(CmfArchetype type) {
		this(type != null ? type.name() : "(null-value)");
	}
}