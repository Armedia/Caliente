package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class Mapping {

	@XmlValue
	protected String value;

	@XmlAttribute(name = "tgt", required = true)
	protected String tgt;

	@XmlAttribute(name = "caseSensitive", required = false)
	protected Boolean caseSensitive;

	@XmlAttribute(name = "separator", required = false)
	protected String separator;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getTgt() {
		return this.tgt;
	}

	public void setTgt(String value) {
		this.tgt = value;
	}

	public boolean isCaseSensitive() {
		return Tools.coalesce(this.caseSensitive, Boolean.TRUE);
	}

	public void setCaseSensitive(Boolean value) {
		this.caseSensitive = value;
	}

	public String getSeparator() {
		return Tools.coalesce(this.separator, ",");
	}

	public void setSeparator(String value) {
		this.separator = value;
	}
}