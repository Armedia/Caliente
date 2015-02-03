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
public abstract class ShptContentObject<T> extends ShptObject<T> {

	private final String id;

	protected ShptContentObject(Service service, T wrapped, StoredObjectType type) {
		super(service, wrapped, type);
		// TODO: this is not mathematically sound for both types. Though the likelihood of
		// actual collisions is (very) low, there is no certainty that the IDs will be unique
		// globally. Thus, the JDBC engine must be revised to account for this.
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