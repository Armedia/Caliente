package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.model.UcmModel.ObjectHandler;

public class UcmFolder extends UcmFSObject {

	UcmFolder(UcmModel model, URI uri, UcmAttributes data) {
		super(model, uri, data, UcmAtt.fFolderName);
	}

	@Override
	public UcmFolder getParentFolder(UcmSession s) throws UcmFolderNotFoundException, UcmServiceException {
		if (isRoot()) { return null; }
		return super.getParentFolder(s);
	}

	public boolean isRoot() {
		return UcmModel.ROOT_URI.equals(getURI());
	}

	public String getDisplayDescription() throws UcmException {
		return getString(UcmAtt.fDisplayDescription);
	}

	public String getFolderDescription() throws UcmException {
		return getString(UcmAtt.fFolderDescription);
	}

	public Map<String, UcmFSObject> getContents(UcmSession s) throws UcmFolderNotFoundException, UcmServiceException {
		return s.getFolderContents(this);
	}

	public int iterateFolderContents(final UcmSession s, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return s.iterateFolderContents(this, handler);
	}

	public int iterateFolderContentsRecursive(final UcmSession s, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return iterateFolderContentsRecursive(s, false, handler);
	}

	public int iterateFolderContentsRecursive(final UcmSession s, boolean recurseShortcuts, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return s.iterateFolderContentsRecursive(this, recurseShortcuts, handler);
	}

	public Collection<UcmFSObject> getFolderContentsRecursive(final UcmSession s, boolean followShortCuts)
		throws UcmServiceException, UcmFolderNotFoundException {
		return s.getFolderContentsRecursive(this, followShortCuts);
	}
}