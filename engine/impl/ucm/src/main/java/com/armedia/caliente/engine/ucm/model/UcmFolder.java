package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Map;

import com.armedia.caliente.engine.ucm.UcmSession;

public class UcmFolder extends UcmFSObject {

	UcmFolder(UcmModel model, URI uri, UcmAttributes data) {
		super(model, uri, data, UcmAtt.fFolderName);
	}

	public String getDisplayDescription() throws UcmException {
		return getString(UcmAtt.fDisplayDescription);
	}

	public String getFolderDescription() throws UcmException {
		return getString(UcmAtt.fFolderDescription);
	}

	public Map<String, UcmFSObject> getContents(UcmSession s) throws UcmFolderNotFoundException, UcmServiceException {
		return this.model.getFolderContents(s, this);
	}
}