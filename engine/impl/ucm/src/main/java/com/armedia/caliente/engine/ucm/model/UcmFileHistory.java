package com.armedia.caliente.engine.ucm.model;

import java.util.Iterator;

public class UcmFileHistory extends UcmModelObject implements Iterable<UcmFile> {

	public UcmFileHistory(UcmModel model) {
		super(model);
	}

	public UcmFile getFile(int revision) {
		return null;
	}

	public UcmFile getFirstRevision() {
		return null;
	}

	public int getRevisionCount() {
		return 0;
	}

	public int getLastRevisionIndex() {
		return 0;
	}

	public UcmFile getLastRevision() {
		return null;
	}

	public int getLastReleasedRevisionIndex() {
		return 0;
	}

	public UcmFile getLastReleasedRevision() {
		return null;
	}

	@Override
	public Iterator<UcmFile> iterator() {
		return null;
	}

	@Override
	public void refresh() throws UcmException {
		this.model.refresh(this);
	}
}