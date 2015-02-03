package com.armedia.cmf.engine.sharepoint;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.FileNameTools;
import com.independentsoft.share.File;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;

public class ShptFolder extends ShptFSObject<Folder> {

	public ShptFolder(Service service, Folder folder) {
		super(service, folder, StoredObjectType.FOLDER);
	}

	public List<String> getContentTypeOrders() {
		return this.wrapped.getContentTypeOrders();
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
		// ObjectID
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_ID.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getServerRelativeUrl()))));

		// Name
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_NAME.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getName()))));

		Date d = this.wrapped.getCreatedTime();
		if (d != null) {
			object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.CREATE_DATE.name, StoredDataType.TIME,
				false, Collections.singleton(new StoredValue(d))));
		}

		d = this.wrapped.getLastModifiedTime();
		if (d != null) {
			object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.MODIFICATION_DATE.name,
				StoredDataType.TIME, false, Collections.singleton(new StoredValue(d))));
		}

		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.WELCOME_PAGE.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getWelcomePage()))));

		// Target Paths
		final String path = FileNameTools.dirname(this.wrapped.getServerRelativeUrl());
		object.setProperty(new StoredProperty<StoredValue>(ShptProperties.TARGET_PATHS.name, StoredDataType.STRING,
			true, Collections.singleton(new StoredValue(path))));
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(Service session, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(session, marshaled, ctx);
		String parentPath = FileNameTools.dirname(this.wrapped.getServerRelativeUrl());
		try {
			ret.add(new ShptFolder(session, session.getFolder(parentPath)));
		} catch (ServiceException e) {
			// TODO: We need to be clearer on the errors being returned... but the API ties our
			// hands and thus we will eventually have to replace it with something better.
			// No parent...
		}
		return ret;
	}

	@Override
	protected Collection<ShptObject<?>> findDependents(Service service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findDependents(service, marshaled, ctx);
		ExportTarget referrent = ctx.getReferrent();
		if (referrent != null) {
			// If the referrent object - i.e. the object that caused us to be added - is a child,
			// then we don't recurse into the other children.
			String referrentPath = referrent.getSearchKey();
			String myPath = this.wrapped.getServerRelativeUrl();
			// If the referrentPath starts with myPath plus a slash, then we can be 100% certain
			// that there is a descendency relationship, and thus we shouldn't recurse.
			if (referrentPath.startsWith(myPath)) { return ret; }
		}
		List<File> files = Collections.emptyList();
		try {
			files = service.getFiles(this.wrapped.getServerRelativeUrl());
		} catch (ServiceException e) {
			files = Collections.emptyList();
		}
		for (File f : files) {
			ret.add(new ShptFile(service, f));
		}
		List<Folder> folders = Collections.emptyList();
		try {
			folders = service.getFolders(this.wrapped.getServerRelativeUrl());
		} catch (ServiceException e) {
			folders = Collections.emptyList();
		}
		for (Folder f : folders) {
			ret.add(new ShptFolder(service, f));
		}
		return ret;
	}
}