package com.armedia.cmf.engine.sharepoint;

import java.util.Date;
import java.util.List;

import com.armedia.cmf.storage.StoredObjectType;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Service;

public class ShptFolder extends ShptContentObject<Folder> {

	public ShptFolder(Service service, Folder folder) {
		super(service, folder, StoredObjectType.FOLDER);
	}

	public List<String> getContentTypeOrders() {
		return this.wrapped.getContentTypeOrders();
	}

	@Override
	public String getId() {
		return this.wrapped.getUniqueId();
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

	public int getItemCount() {
		return this.wrapped.getItemCount();
	}

	public List<String> getUniqueContentTypeOrders() {
		return this.wrapped.getUniqueContentTypeOrders();
	}

	public String getWelcomePage() {
		return this.wrapped.getWelcomePage();
	}

	public void setWelcomePage(String welcomePage) {
		this.wrapped.setWelcomePage(welcomePage);
	}
}