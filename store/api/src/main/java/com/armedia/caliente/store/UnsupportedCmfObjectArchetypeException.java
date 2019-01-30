/**
 *
 */

package com.armedia.caliente.store;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class UnsupportedCmfObjectArchetypeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	UnsupportedCmfObjectArchetypeException(String type) {
		super(String.format("The object archetype [%s] is not supported", type));
	}

	public UnsupportedCmfObjectArchetypeException(CmfObject.Archetype type) {
		this(type != null ? type.name() : "(null-value)");
	}
}