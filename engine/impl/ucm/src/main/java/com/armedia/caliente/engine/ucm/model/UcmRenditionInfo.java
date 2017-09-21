package com.armedia.caliente.engine.ucm.model;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet.Field;

public final class UcmRenditionInfo {

	public static final String PRIMARY = "primary";
	public static final String DEFAULT = UcmRenditionInfo.PRIMARY;

	private final UcmUniqueURI guid;
	private final String type;
	private final String format;
	private final String name;
	private final String description;

	UcmRenditionInfo(UcmUniqueURI guid, DataObject obj, Collection<Field> structure) {
		UcmAttributes data = new UcmAttributes(obj, structure);
		this.guid = guid;
		this.type = data.getString(UcmAtt.rendType);
		this.format = data.getString(UcmAtt.rendFormat);
		this.name = data.getString(UcmAtt.rendName);
		this.description = data.getString(UcmAtt.rendDescription);
	}

	public UcmUniqueURI getGuid() {
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

	public boolean isPrimary() {
		return StringUtils.equalsIgnoreCase(this.name, UcmRenditionInfo.PRIMARY);
	}

	public boolean isDefault() {
		return isPrimary();
	}
}