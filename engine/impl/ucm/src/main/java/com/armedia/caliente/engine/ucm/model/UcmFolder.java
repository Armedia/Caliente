package com.armedia.caliente.engine.ucm.model;

import java.util.Iterator;

import com.armedia.caliente.engine.ucm.IdcSession;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataObject;

public class UcmFolder extends UcmFSObject {

	UcmFolder(UcmModel model, DataObject data) {
		super(model, data, UcmAtt.fFolderName, UcmAtt.fFolderGUID);
	}

	public String getDisplayDescription() throws UcmException {
		return getString(UcmAtt.fDisplayDescription);
	}

	public String getFolderDescription() throws UcmException {
		return getString(UcmAtt.fFolderDescription);
	}

	public Iterator<UcmFSObject> getChildren(IdcSession session, boolean lazy) throws IdcClientException {
		return null;
	}

	@Override
	public void refresh() throws UcmException {
		this.model.refresh(this);
	}
}