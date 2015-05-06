package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.File;
import com.independentsoft.share.Folder;

public class ShptFolder extends ShptFSObject<Folder> {

	public ShptFolder(ShptExportDelegateFactory factory, Folder object) throws Exception {
		super(factory, Folder.class, object);
	}

	public List<String> getContentTypeOrders() {
		return this.object.getContentTypeOrders();
	}

	@Override
	public String calculateServerRelativeUrl(Folder f) {
		return f.getServerRelativeUrl();
	}

	@Override
	public String getName() {
		return this.object.getName();
	}

	@Override
	public Date getCreatedTime() {
		return this.object.getCreatedTime();
	}

	@Override
	public Date getLastModifiedTime() {
		return this.object.getLastModifiedTime();
	}

	public int getItemCount() {
		return this.object.getItemCount();
	}

	public List<String> getUniqueContentTypeOrders() {
		return this.object.getUniqueContentTypeOrders();
	}

	public String getWelcomePage() {
		return this.object.getWelcomePage();
	}

	public void setWelcomePage(String welcomePage) {
		this.object.setWelcomePage(welcomePage);
	}

	@Override
	public String calculateBatchId(Folder f) {
		// Count the number of levels down this path is
		Collection<String> ret = FileNameTools.tokenize(f.getServerRelativeUrl(), '/');
		return String.format("%016x", ret.size());
	}

	@Override
	public String calculateLabel(Folder f) {
		return f.getServerRelativeUrl();
	}

	@Override
	protected void marshal(ShptExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		// TODO Auto-generated method stub
		super.marshal(ctx, object);
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.WELCOME_PAGE.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.object.getWelcomePage()))));
	}

	@Override
	protected Collection<ShptObject<?>> findDependents(ShptSession service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findDependents(service, marshaled, ctx);
		ExportTarget referrent = ctx.getReferrent();
		if (referrent != null) {
			// If the referrent object - i.e. the object that caused us to be added - is a child,
			// then we don't recurse into the other children.
			String referrentPath = referrent.getSearchKey();
			String myPath = this.object.getServerRelativeUrl();
			// If the referrentPath starts with myPath plus a slash, then we can be 100% certain
			// that there is a descendency relationship, and thus we shouldn't recurse.
			if (referrentPath.startsWith(myPath)) { return ret; }
		}
		List<File> files = Collections.emptyList();
		try {
			files = service.getFiles(this.object.getServerRelativeUrl());
		} catch (ShptSessionException e) {
			files = Collections.emptyList();
		}
		for (File f : files) {
			ret.add(new ShptFile(this.factory, f));
		}
		List<Folder> folders = Collections.emptyList();
		try {
			folders = service.getFolders(this.object.getServerRelativeUrl());
		} catch (ShptSessionException e) {
			folders = Collections.emptyList();
		}
		for (Folder f : folders) {
			ret.add(new ShptFolder(this.factory, f));
		}
		return ret;
	}

	static String doCalculateObjectId(Folder object) {
		String searchKey = object.getServerRelativeUrl();
		return String.format("%08X", Tools.hashTool(searchKey, null, searchKey));
	}

	static String doCalculateSearchKey(Folder object) {
		return object.getServerRelativeUrl();
	}
}