package com.armedia.cmf.engine.local.exporter;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
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
ExportDelegate<URL, URL, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportDelegateFactory, LocalExportEngine> {

	private static final Charset CHARSET = Charset.forName("UTF-8");

	/**
	 * The root path - this will be canonicalized
	 */
	protected final URL root;

	protected LocalExportDelegate(LocalExportDelegateFactory factory, URL object) throws Exception {
		super(factory, URL.class, object);
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
	protected List<ContentInfo> storeContent(URL session, StoredObject<StoredValue> marshalled, ExportTarget referrent,
		ContentStore streamStore) throws Exception {
		return null;
	}

	@Override
	protected StoredObjectType calculateType(URL object) throws Exception {
		File f = new File(object.toURI());
		if (f.isFile()) { return StoredObjectType.DOCUMENT; }
		if (f.isDirectory()) { return StoredObjectType.FOLDER; }
		throw new ExportException(String.format("Filesystem object [%s] is of an unknown type", object));
	}

	@Override
	protected String calculateLabel(URL object) throws Exception {
		// TODO: Calculate the path relative to the root folder
		return object.toString();
	}

	@Override
	protected String calculateObjectId(URL object) throws Exception {
		return null;
	}

	@Override
	protected String calculateSearchKey(URL object) throws Exception {
		URI relative = this.root.toURI().relativize(object.toURI());
		return relative.toURL().toString();
	}
}