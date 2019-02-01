package com.armedia.caliente.engine.local.exporter;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.attribute.UserPrincipalLookupService;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalFile;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportDelegateFactory
	extends ExportDelegateFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportEngine> {

	private final LocalRoot root;
	private final boolean copyContent;
	private final UserPrincipalLookupService userDb;

	protected LocalExportDelegateFactory(LocalExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		File root = LocalCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = new LocalRoot(root);
		this.copyContent = configuration.getBoolean(LocalSetting.COPY_CONTENT);
		this.userDb = FileSystems.getDefault().getUserPrincipalLookupService();
	}

	public final LocalRoot getRoot() {
		return this.root;
	}

	public final boolean isCopyContent() {
		return this.copyContent;
	}

	@Override
	protected LocalExportDelegate<?> newExportDelegate(LocalRoot session, CmfObject.Archetype type, String searchKey)
		throws Exception {
		switch (type) {
			case FOLDER:
			case DOCUMENT:
				return new LocalFileExportDelegate(this, session, LocalFile.newFromSafePath(session, searchKey));
			case USER:
				return new LocalPrincipalExportDelegate(this, session, this.userDb.lookupPrincipalByName(searchKey));
			case GROUP:
				return new LocalPrincipalExportDelegate(this, session,
					this.userDb.lookupPrincipalByGroupName(searchKey));
			default:
				break;
		}
		this.log.warn("Type [{}] is not supported - no delegate created for search key [{}]", type, searchKey);
		return null;
	}
}