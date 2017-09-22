/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import com.armedia.caliente.engine.sharepoint.ShptSession;

/**
 * @author diego
 *
 */
public abstract class ShptSecurityObject<T> extends ShptObject<T> {

	protected ShptSecurityObject(ShptExportDelegateFactory factory, ShptSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected final String calculateSearchKey(ShptSession session, T object) {
		return String.valueOf(calculateNumericId(session, object));
	}

	@Override
	public String calculateObjectId(ShptSession session, T object) throws Exception {
		return String.format("%04x", calculateNumericId(session, object));
	}

	protected abstract int calculateNumericId(ShptSession session, T object);
}