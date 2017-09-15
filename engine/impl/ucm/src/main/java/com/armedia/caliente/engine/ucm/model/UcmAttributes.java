package com.armedia.caliente.engine.ucm.model;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.model.impl.DataObjectEncodingUtils;

public final class UcmAttributes {

	private final Map<String, String> data;

	UcmAttributes(Map<String, String> data) {
		if (data == null) {
			data = new HashMap<>();
		} else {
			data = new HashMap<>(data);
		}
		this.data = data;
	}

	public Map<String, String> getData() {
		return this.data;
	}

	public final boolean hasAttribute(UcmAtt att) {
		return this.data.containsKey(att.name());
	}

	public final String getString(UcmAtt att) {
		return getString(att, null);
	}

	public final String getString(UcmAtt att, String def) {
		return Tools.coalesce(this.data.get(att.name()), def);
	}

	public final Date getDate(UcmAtt att) {
		return getDate(att, null);
	}

	public final Date getDate(UcmAtt att, Date def) {
		Calendar c = getCalendar(att);
		if (c == null) { return def; }
		return c.getTime();
	}

	public final Calendar getCalendar(UcmAtt att) {
		return getCalendar(att, null);
	}

	public final Calendar getCalendar(UcmAtt att, Calendar def) {
		String rawDate = getString(att);
		if (rawDate == null) { return def; }
		try {
			return DataObjectEncodingUtils.decodeDate(rawDate);
		} catch (ParseException e) {
			throw new UcmRuntimeException(String.format("Failed to parse out the date [%s]", rawDate), e);
		}
	}

	public final Integer getInteger(UcmAtt att) {
		return CfgTools.decodeInteger(att.name(), this.data);
	}

	public final int getInteger(UcmAtt att, int def) {
		Integer v = getInteger(att);
		return (v != null ? v.intValue() : def);
	}

	public final Boolean getBoolean(UcmAtt att) {
		return Tools.toBoolean(getString(att));
	}

	public final boolean getBoolean(UcmAtt att, boolean def) {
		Boolean b = getBoolean(att);
		return (b != null ? b.booleanValue() : def);
	}
}