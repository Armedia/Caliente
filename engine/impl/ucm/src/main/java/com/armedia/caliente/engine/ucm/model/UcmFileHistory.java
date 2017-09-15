package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class UcmFileHistory extends UcmModelObject implements Iterable<UcmRevision> {

	private final List<UcmRevision> versions;
	private final UcmRevision firstRevision;
	private final UcmRevision lastRevision;

	public UcmFileHistory(UcmModel model, URI uri, List<UcmRevision> versions) {
		super(model, uri);
		this.versions = Tools.freezeCopy(versions);
		this.firstRevision = this.versions.get(0);
		this.lastRevision = this.versions.get(this.versions.size() - 1);
	}

	public UcmRevision getVersion(int revision) {
		return this.versions.get(revision);
	}

	public UcmRevision getFirstRevision() {
		return this.firstRevision;
	}

	public int getRevisionCount() {
		return this.versions.size();
	}

	public UcmRevision getLastRevision() {
		return this.lastRevision;
	}

	@Override
	public Iterator<UcmRevision> iterator() {
		return this.versions.iterator();
	}
}