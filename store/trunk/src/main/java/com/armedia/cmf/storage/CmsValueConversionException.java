/**
 *
 */

package com.armedia.cmf.storage;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public final class CmsValueConversionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final CmsDataType source;
	private final CmsDataType target;

	private final Object value;

	CmsValueConversionException(CmsDataType source, CmsDataType target, Object value) {
		this(source, target, value, null);
	}

	CmsValueConversionException(CmsDataType source, CmsDataType target, Object value, Throwable cause) {
		super(String.format("Can't convert from %s to %s", source, target), cause);
		this.source = source;
		this.target = target;
		this.value = value;
	}

	public CmsDataType getSource() {
		return this.source;
	}

	public CmsDataType getTarget() {
		return this.target;
	}

	public Object getValue() {
		return this.value;
	}
}