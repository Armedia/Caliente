/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public abstract class ShptSecurityObject<T> extends ShptObject<T> {

	protected ShptSecurityObject(Service service, T wrapped, StoredObjectType type) {
		super(service, wrapped, type);
	}

	@Override
	public final String getId() {
		return String.format("%04x", getNumericId());
	}

	protected abstract int getNumericId();

	@Override
	protected void marshal(StoredObject<StoredValue> object) throws ExportException {
		// TODO Auto-generated method stub

	}
}