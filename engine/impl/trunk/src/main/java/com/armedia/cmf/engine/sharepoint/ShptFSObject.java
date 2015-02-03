/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.util.Date;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public abstract class ShptFSObject<T> extends ShptObject<T> {

	private final String id;

	protected ShptFSObject(Service service, T wrapped, StoredObjectType type) {
		super(service, wrapped, type);
		this.id = String.format("%08X", Tools.hashTool(this, null, type, getSearchKey()));
	}

	@Override
	public final String getId() {
		return this.id;
	}

	public abstract String getServerRelativeUrl();

	public abstract Date getCreatedTime();

	public abstract Date getLastModifiedTime();
}