package com.armedia.caliente.engine.ucm.exporter;

import java.util.List;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.commons.utilities.FileNameTools;

public class UcmFolderExportDelegate extends UcmFSObjectExportDelegate<UcmFolder> {

	protected UcmFolderExportDelegate(UcmExportDelegateFactory factory, UcmSession session, UcmFolder object)
		throws Exception {
		super(factory, session, UcmFolder.class, object);
	}

	@Override
	protected int calculateDependencyTier(UcmSession session, UcmFolder folder) throws Exception {
		final UcmFolder original = folder;

		if (folder.isShortcut()) {
			folder = session.getFolderByGUID(folder.getTargetGUID());
		}

		String path = folder.getPath();
		List<String> l = FileNameTools.tokenize(path, '/');
		int depth = l.size();
		if (original.isShortcut()) {
			depth++;
		}
		return depth;
	}
}