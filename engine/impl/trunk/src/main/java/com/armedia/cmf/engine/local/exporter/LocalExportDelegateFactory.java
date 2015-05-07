package com.armedia.cmf.engine.local.exporter;

import java.io.File;
import java.net.URL;

import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportDelegateFactory extends
ExportDelegateFactory<URL, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportEngine> {

	private final URL root;

	protected LocalExportDelegateFactory(LocalExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		File root = LocalCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = root.toURI().toURL();
	}

	public final URL getRoot() {
		return this.root;
	}

	@Override
	protected LocalExportDelegate newExportDelegate(URL session, StoredObjectType type, String searchKey)
		throws Exception {
		return null;
	}
}