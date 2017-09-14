package com.armedia.caliente.engine.ucm.model;

import java.util.Calendar;
import java.util.Date;

import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.model.DataObject;

public class UcmTools {

	private final DataObject dataObject;

	public UcmTools(DataObject data) {
		this.dataObject = data;
	}

	public final DataObject getDataObject() {
		return this.dataObject;
	}

	public final boolean hasAttribute(UcmAtt att) {
		return UcmTools.hasAttribute(this.dataObject, att);
	}

	public final String getString(UcmAtt att) {
		return UcmTools.getString(this.dataObject, att);
	}

	public final String getString(UcmAtt att, String def) {
		return UcmTools.getString(this.dataObject, att, def);
	}

	public final Date getDate(UcmAtt att) {
		return UcmTools.getDate(this.dataObject, att);
	}

	public final Date getDate(UcmAtt att, Date def) {
		return UcmTools.getDate(this.dataObject, att, def);
	}

	public final Calendar getCalendar(UcmAtt att) {
		return UcmTools.getCalendar(this.dataObject, att);
	}

	public final Calendar getCalendar(UcmAtt att, Calendar def) {
		return UcmTools.getCalendar(this.dataObject, att, def);
	}

	public final Integer getInteger(UcmAtt att) {
		return UcmTools.getInteger(this.dataObject, att);
	}

	public final int getInteger(UcmAtt att, int def) {
		return UcmTools.getInteger(this.dataObject, att, def);
	}

	public final Boolean getBoolean(UcmAtt att) {
		return UcmTools.getBoolean(this.dataObject, att);
	}

	public final boolean getBoolean(UcmAtt att, boolean def) {
		return UcmTools.getBoolean(this.dataObject, att, def);
	}

	public static final boolean hasAttribute(DataObject data, UcmAtt att) {
		return data.containsKey(att.name());
	}

	public static final String getString(DataObject data, UcmAtt att) {
		return UcmTools.getString(data, att, null);
	}

	public static final String getString(DataObject data, UcmAtt att, String def) {
		return Tools.coalesce(data.get(att.name()), def);
	}

	public static final Date getDate(DataObject data, UcmAtt att) {
		return UcmTools.getDate(data, att, null);
	}

	public static final Date getDate(DataObject data, UcmAtt att, Date def) {
		return Tools.coalesce(data.getDate(att.name()), def);
	}

	public static final Calendar getCalendar(DataObject data, UcmAtt att) {
		return UcmTools.getCalendar(data, att, null);
	}

	public static final Calendar getCalendar(DataObject data, UcmAtt att, Calendar def) {
		return Tools.coalesce(data.getCalendar(att.name()), def);
	}

	public static final Integer getInteger(DataObject data, UcmAtt att) {
		return data.getInteger(att.name());
	}

	public static final int getInteger(DataObject data, UcmAtt att, int def) {
		Integer v = UcmTools.getInteger(data, att);
		return (v != null ? v.intValue() : def);
	}

	public static final Boolean getBoolean(DataObject data, UcmAtt att) {
		return Tools.toBoolean(UcmTools.getString(data, att));
	}

	public static final boolean getBoolean(DataObject data, UcmAtt att, boolean def) {
		Boolean b = UcmTools.getBoolean(data, att);
		return (b != null ? b.booleanValue() : def);
	}
}