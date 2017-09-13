package com.armedia.caliente.engine.ucm.model;

import java.util.Iterator;

import com.armedia.caliente.engine.ucm.IdcSession;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataObject;

public class UcmFolder extends UcmFSObject {

	public UcmFolder(DataObject data) {
		this(null, data);
	}

	UcmFolder(UcmFolder parent, DataObject data) {
		super(parent, data, UcmAtt.fFolderName, UcmAtt.fFolderGUID);
	}

	public String getDisplayDescription() {
		return getString(UcmAtt.fDisplayDescription);
	}

	public String getFolderDescription() {
		return getString(UcmAtt.fFolderDescription);
	}

	public Iterator<UcmFSObject> getChildren(IdcSession session, boolean lazy) throws IdcClientException {
		return null;
	}
}