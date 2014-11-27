/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.util.Date;

import com.armedia.cmf.storage.StoredObjectType;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public abstract class ShptContentObject<T> extends ShptObject<T> {

	protected ShptContentObject(Service service, T wrapped, StoredObjectType type) {
		super(service, wrapped, type);
	}

	public abstract String getServerRelativeUrl();

	public abstract Date getCreatedTime();

	public abstract Date getLastModifiedTime();
}