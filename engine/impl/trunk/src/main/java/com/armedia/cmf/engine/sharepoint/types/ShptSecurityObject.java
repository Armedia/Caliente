/**
 *
 */

package com.armedia.cmf.engine.sharepoint.types;

import com.armedia.cmf.engine.sharepoint.exporter.ShptExportEngine;

/**
 * @author diego
 *
 */
public abstract class ShptSecurityObject<T> extends ShptObject<T> {

	protected ShptSecurityObject(ShptExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
	}

	@Override
	protected final String calculateSearchKey(T object) {
		return String.valueOf(calculateNumericId(object));
	}

	@Override
	public String calculateObjectId(T object) throws Exception {
		return String.format("%04x", calculateNumericId(object));
	}

	protected abstract int calculateNumericId(T object);
}