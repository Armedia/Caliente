/**
 *
 */

package com.armedia.caliente.store;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class UnsupportedCmfArchetypeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	UnsupportedCmfArchetypeException(String type) {
		super(String.format("The object archetype [%s] is not supported", type));
	}

	public UnsupportedCmfArchetypeException(CmfArchetype type) {
		this(type != null ? type.name() : "(null-value)");
	}
}