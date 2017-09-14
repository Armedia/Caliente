package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.util.Set;

import oracle.stellent.ridc.model.DataObject;

public class UcmFile extends UcmFSObject {

	UcmFile(UcmModel model, DataObject data) {
		super(model, data, UcmAtt.fFileName, UcmAtt.fFileGUID);
	}

	public String getPublishedFileName() throws UcmException {
		return getString(UcmAtt.fPublishedFilename);
	}

	public String getOriginalName() throws UcmException {
		return getString(UcmAtt.dOriginalName);
	}

	public String getAuthor() throws UcmException {
		return getString(UcmAtt.dDocAuthor);
	}

	public String getTitle() throws UcmException {
		return getString(UcmAtt.dDocTitle);
	}

	public int getSize() throws UcmException {
		return getInteger(UcmAtt.dFileSize, 0);
	}

	public String getFormat() throws UcmException {
		return getString(UcmAtt.dFormat);
	}

	public int getRevisionId() throws UcmException {
		return getInteger(UcmAtt.dID, 1);
	}

	public int getRevisionNumber() throws UcmException {
		return getInteger(UcmAtt.dRevisionID, 1);
	}

	public int getPublishedRevisionId() throws UcmException {
		return getInteger(UcmAtt.dPublishedRevisionID, 1);
	}

	public String getRevisionLabel() throws UcmException {
		return getString(UcmAtt.dRevLabel);
	}

	public String getExtension() throws UcmException {
		return getString(UcmAtt.dExtension);
	}

	public String getContentId() throws UcmException {
		return getString(UcmAtt.dDocName);
	}

	public Set<String> getRenditionNames() throws UcmException {
		return null;
	}

	public InputStream getInputStream() throws UcmException {
		return getInputStream(null);
	}

	public InputStream getInputStream(String rendition) throws UcmException {
		return this.model.getInputStream(this, rendition);
	}

	@Override
	public void refresh() throws UcmException {
		this.model.refresh(this);
	}
}