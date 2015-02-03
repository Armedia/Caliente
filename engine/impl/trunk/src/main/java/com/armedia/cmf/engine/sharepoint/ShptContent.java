/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.util.Date;

import com.armedia.cmf.storage.StoredObjectType;
import com.independentsoft.share.File;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptContent extends ShptFSObject<File> {

	public ShptContent(Service service, File wrapped) {
		super(service, wrapped, StoredObjectType.CONTENT);
	}

	@Override
	public String getSearchKey() {
		return this.wrapped.getServerRelativeUrl();
	}

	@Override
	public String getName() {
		return this.wrapped.getName();
	}

	@Override
	public String getServerRelativeUrl() {
		return this.wrapped.getServerRelativeUrl();
	}

	@Override
	public Date getCreatedTime() {
		return this.wrapped.getCreatedTime();
	}

	@Override
	public Date getLastModifiedTime() {
		return this.wrapped.getLastModifiedTime();
	}

	@Override
	public String getBatchId() {
		return this.wrapped.getUniqueId();
	}

	@Override
	public String getLabel() {
		return this.wrapped.getName();
	}
}