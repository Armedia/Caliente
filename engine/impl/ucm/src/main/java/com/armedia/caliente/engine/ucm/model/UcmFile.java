package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;

public class UcmFile extends UcmFSObject {

	UcmFile(UcmModel model, URI uri, UcmAttributes data) {
		super(model, uri, data, UcmAtt.fFileName, UcmAtt.fFileGUID);
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

	public String getRevisionId() {
		return getString(UcmAtt.dID);
	}

	public int getRevisionNumber() {
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

	public InputStream getInputStream() throws UcmServiceException, UcmFileRevisionNotFoundException {
		return getInputStream(null);
	}

	public InputStream getInputStream(String rendition) throws UcmServiceException, UcmFileRevisionNotFoundException {
		return this.model.getInputStream(this, rendition);
	}
}