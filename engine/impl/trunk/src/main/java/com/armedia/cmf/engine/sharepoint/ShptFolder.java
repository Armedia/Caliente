package com.armedia.cmf.engine.sharepoint;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.FileNameTools;
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

	@Override
	public String getBatchId() {
		// Count the number of levels down this path is
		Collection<String> ret = FileNameTools.tokenize(this.wrapped.getServerRelativeUrl(), '/');
		return String.format("%016x", ret.size());
	}

	@Override
	public String getLabel() {
		return this.wrapped.getServerRelativeUrl();
	}

	@Override
	protected void marshal(StoredObject<StoredValue> object) throws ExportException {
		ShptAttributes.OBJECT_ID.name();
		this.wrapped.getUniqueId();
		ShptAttributes.OBJECT_NAME.name();
		this.wrapped.getName();

		// DCTM pulls this from a property called targetPaths, for searching by path
		ShptAttributes.PATHS.name();
		this.wrapped.getServerRelativeUrl();

		ShptAttributes.CREATE_DATE.name();
		this.wrapped.getCreatedTime();
		ShptAttributes.MODIFICATION_DATE.name();
		this.wrapped.getLastModifiedTime();

		ShptAttributes.OBJECT_NAME.name();
		this.wrapped.getUniqueContentTypeOrders();
		ShptAttributes.OBJECT_NAME.name();
		this.wrapped.getContentTypeOrders();
		ShptAttributes.OBJECT_NAME.name();
		this.wrapped.getWelcomePage();
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(Service session, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		// Find each of the parents going upwards...
		// TODO Auto-generated method stub
		return super.findRequirements(session, marshaled, ctx);
	}
}