package com.armedia.caliente.engine.ucm.model;

import java.net.URI;

import oracle.stellent.ridc.model.DataObject;

public class UcmRevision {

	public static enum Status {
		//
		RELEASED,
		//
		;
	}

	private final URI uri;
	private final String format;
	private final String id;
	private final String processingState;
	private final String revLabel;
	private final int revisionId;
	private final Status status;

	UcmRevision(DataObject obj) {
		UcmTools data = new UcmTools(obj);
		this.uri = UcmModel.getURI(obj);
		this.format = data.getString(UcmAtt.dFormat);
		this.id = data.getString(UcmAtt.dID);
		this.processingState = data.getString(UcmAtt.dProcessingState);
		this.revLabel = data.getString(UcmAtt.dRevLabel);
		this.revisionId = data.getInteger(UcmAtt.dRevisionID);
		this.status = Status.valueOf(data.getString(UcmAtt.dStatus).toUpperCase());
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