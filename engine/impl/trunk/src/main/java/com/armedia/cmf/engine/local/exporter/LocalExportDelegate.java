package com.armedia.cmf.engine.local.exporter;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;

public class LocalExportDelegate
extends
	ExportDelegate<File, File, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportDelegateFactory, LocalExportEngine> {

	protected final File root;

	protected LocalExportDelegate(LocalExportDelegateFactory factory, File object) throws Exception {
		super(factory, File.class, object.getCanonicalFile());
		this.root = factory.getRoot();
	}

	@Override
	protected Collection<LocalExportDelegate> identifyRequirements(StoredObject<StoredValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected void marshal(LocalExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
	}

	@Override
	protected Collection<LocalExportDelegate> identifyDependents(StoredObject<StoredValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected List<ContentInfo> storeContent(File session, StoredObject<StoredValue> marshalled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		return null;
	}

	@Override
	protected StoredObjectType calculateType(File object) throws Exception {
		if (object.isFile()) { return StoredObjectType.DOCUMENT; }
		if (object.isDirectory()) { return StoredObjectType.FOLDER; }
		throw new ExportException(String.format("Filesystem object [%s] is of an unknown type",
			this.object.getAbsolutePath()));
	}

	@Override
	protected String calculateLabel(File object) throws Exception {
		// TODO: Calculate the path relative to the root folder
		return object.getAbsolutePath();
	}

	@Override
	protected String calculateObjectId(File object) throws Exception {
		// TODO: Calculate the path relative to the root folder
		return null;
	}

	@Override
	protected String calculateSearchKey(File object) throws Exception {
		final String fullPath = object.getCanonicalPath();
		final String fullRoot = this.root.getCanonicalPath();
		return null;
	}
}