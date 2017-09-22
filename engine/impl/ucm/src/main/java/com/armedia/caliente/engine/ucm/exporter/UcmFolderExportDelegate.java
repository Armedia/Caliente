package com.armedia.caliente.engine.ucm.exporter;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.model.UcmFolder;

public class UcmFolderExportDelegate extends UcmFSObjectExportDelegate<UcmFolder> {

	protected UcmFolderExportDelegate(UcmExportDelegateFactory factory, UcmSession session, UcmFolder object)
		throws Exception {
		super(factory, session, UcmFolder.class, object);
	}
}