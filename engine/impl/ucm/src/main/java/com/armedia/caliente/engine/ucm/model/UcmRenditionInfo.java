package com.armedia.caliente.engine.ucm.model;

import oracle.stellent.ridc.model.DataObject;

public class UcmRenditionInfo {

	private final UcmGUID guid;
	private final String type;
	private final String format;
	private final String name;
	private final String description;

	UcmRenditionInfo(UcmGUID guid, DataObject obj) {
		UcmTools data = new UcmTools(obj);
		this.guid = guid;
		this.type = data.getString(UcmAtt.rendType);
		this.format = data.getString(UcmAtt.rendFormat);
		this.name = data.getString(UcmAtt.rendName);
		this.description = data.getString(UcmAtt.rendDescription);
	}

	public UcmGUID getGuid() {
		return this.guid;
	}

	public String getType() {
		return this.type;
	}

	public String getFormat() {
		return this.format;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}
}