package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.util.Set;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataObject;

public class UcmFile extends UcmFSObject {

	UcmFile(UcmModel model, DataObject data) {
		super(model, data, UcmAtt.fFileName, UcmAtt.fFileGUID);
	}

	public String getPublishedFileName() {
		return getString(UcmAtt.fPublishedFilename);
	}

	public String getOriginalName() {
		return getString(UcmAtt.dOriginalName);
	}

	public String getAuthor() {
		return getString(UcmAtt.dDocAuthor);
	}

	public String getTitle() {
		return getString(UcmAtt.dDocTitle);
	}

	public int getSize() {
		return getInteger(UcmAtt.dFileSize, 0);
	}

	public String getFormat() {
		return getString(UcmAtt.dFormat);
	}

	public int getRevisionId() {
		return getInteger(UcmAtt.dRevisionID, 1);
	}

	public int getPublishedRevisionId() {
		return getInteger(UcmAtt.dPublishedRevisionID, 1);
	}

	public String getRevisionLabel() {
		return getString(UcmAtt.dRevLabel);
	}

	public String getExtension() {
		return getString(UcmAtt.dExtension);
	}

	public String getContentId() {
		return getString(UcmAtt.dDocName);
	}

	public Set<String> getRenditionNames() {
		return null;
	}

	public InputStream getInputStream() throws IdcClientException {
		return getInputStream(null);
	}

	public InputStream getInputStream(String rendition) throws IdcClientException {
		return this.model.getInputStream(this, rendition);
	}

	@Override
	public void refresh() throws IdcClientException {
		this.model.refresh(this);
	}
}