package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class UcmFileHistory extends UcmModelObject implements Iterable<UcmFile> {

	private final List<UcmFile> files;
	private final UcmFile firstRevision;
	private final UcmFile lastRevision;

	public UcmFileHistory(UcmModel model, URI uri, List<UcmFile> files) {
		super(model, uri);
		this.files = Tools.freezeCopy(files);
		this.firstRevision = this.files.get(0);
		this.lastRevision = this.files.get(this.files.size() - 1);
	}

	public UcmFile getFile(int revision) {
		return this.files.get(revision);
	}

	public UcmFile getFirstRevision() {
		return this.firstRevision;
	}

	public int getRevisionCount() {
		return this.files.size();
	}

	public UcmFile getLastRevision() {
		return this.lastRevision;
	}

	@Override
	public Iterator<UcmFile> iterator() {
		return this.files.iterator();
	}
}