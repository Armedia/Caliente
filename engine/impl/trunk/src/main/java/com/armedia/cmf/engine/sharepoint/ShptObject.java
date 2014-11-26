/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.storage.StoredObjectType;

/**
 * @author diego
 *
 */
public abstract class ShptObject<T> {

	protected final T wrapped;

	protected ShptObject(T wrapped) {
		if (wrapped == null) { throw new IllegalArgumentException("Must provide an object to wrap around"); }
		this.wrapped = wrapped;
	}

	public final T getObject() {
		return this.wrapped;
	}

	public abstract String getId();

	public abstract StoredObjectType getStoredType();
}