package com.armedia.caliente.engine.dynamic.xml.mapper;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class Mapping extends MappingElement {

	public static final Character DEFAULT_SEP = ',';
	public static final String DEFAULT_SEP_STR = Mapping.DEFAULT_SEP.toString();

	@XmlAttribute(name = "tgt", required = true)
	protected String tgt;

	@XmlAttribute(name = "caseSensitive", required = false)
	protected Boolean caseSensitive;

	@XmlAttribute(name = "override", required = false)
	protected Boolean override;

	@XmlAttribute(name = "separator", required = false)
	protected String separator;

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		if (this.separator != null) {
			if (this.separator.length() > 1) {
				// Only use the first character
				this.separator = this.separator.substring(0, 1);
			} else {
				this.separator = null;
			}
		}
	}

	public String getTgt() {
		return this.tgt;
	}

	public void setTgt(String value) {
		this.tgt = value;
	}

	public boolean isOverride() {
		return Tools.coalesce(this.override, Boolean.FALSE);
	}

	public void setOverride(Boolean value) {
		this.override = value;
	}

	public boolean isCaseSensitive() {
		return Tools.coalesce(this.caseSensitive, Boolean.TRUE);
	}

	public void setCaseSensitive(Boolean value) {
		this.caseSensitive = value;
	}

	public char getSeparator() {
		if (this.separator == null) { return Mapping.DEFAULT_SEP; }
		return this.separator.charAt(0);
	}

	public void setSeparator(Character value) {
		if (value == null) {
			this.separator = null;
		} else {
			this.separator = value.toString();
		}
	}
}