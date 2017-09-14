package com.armedia.caliente.engine.ucm.model;

import java.net.URI;

import oracle.stellent.ridc.model.DataObject;

public class UcmVersionInfo {

	public static enum Status {
		//
		RELEASED,
		//
		;
	}

	private final String author;
	private final URI uri;
	private final String format;
	private final String id;
	private final String processingState;
	private final Status status;
	private final String revLabel;
	private final int revisionId;

	UcmVersionInfo(DataObject obj) {
		UcmTools data = new UcmTools(obj);
		this.author = data.getString(UcmAtt.dDocAuthor);
		this.format = data.getString(UcmAtt.dFormat);
		this.id = data.getString(UcmAtt.dID);
		this.processingState = data.getString(UcmAtt.dProcessingState);
		this.status = Status.valueOf(data.getString(UcmAtt.dStatus).toUpperCase());
		this.revLabel = data.getString(UcmAtt.dRevLabel);
		this.revisionId = data.getInteger(UcmAtt.dRevisionID);
		this.uri = UcmModel.newURI(UcmModel.FILE_SCHEME, data.getString(UcmAtt.dDocName));
	}

	public String getAuthor() {
		return this.author;
	}

	public URI getUri() {
		return this.uri;
	}

	public String getFormat() {
		return this.format;
	}

	public String getId() {
		return this.id;
	}

	public String getProcessingState() {
		return this.processingState;
	}

	public Status getStatus() {
		return this.status;
	}

	public String getRevLabel() {
		return this.revLabel;
	}

	public int getRevisionId() {
		return this.revisionId;
	}
}