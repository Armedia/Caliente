package com.armedia.caliente.engine.ucm.exporter;

import com.armedia.caliente.engine.ucm.model.UcmFolder;

public class UcmFolderExportDelegate extends UcmFSObjectExportDelegate<UcmFolder> {

	protected UcmFolderExportDelegate(UcmExportDelegateFactory factory, UcmFolder object) throws Exception {
		super(factory, UcmFolder.class, object);
	}
}