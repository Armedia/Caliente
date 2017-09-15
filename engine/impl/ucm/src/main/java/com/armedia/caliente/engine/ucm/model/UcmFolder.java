package com.armedia.caliente.engine.ucm.model;

import java.net.URI;

public class UcmFolder extends UcmFSObject {

	UcmFolder(UcmModel model, URI uri, UcmAttributes data) {
		super(model, uri, data, UcmAtt.fFolderName, UcmAtt.fFolderGUID);
	}

	public String getDisplayDescription() throws UcmException {
		return getString(UcmAtt.fDisplayDescription);
	}

	public String getFolderDescription() throws UcmException {
		return getString(UcmAtt.fFolderDescription);
	}
}