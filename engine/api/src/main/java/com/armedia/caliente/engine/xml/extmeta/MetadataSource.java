package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSource.t", propOrder = {
	"settings", "metadata"
})
public class MetadataSource {

	@XmlElementWrapper(name = "settings")
	@XmlElement(name = "setting", required = true)
	protected List<SettingT> settings;

	@XmlElement(required = true)
	protected Metadata metadata;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "failOnError", required = false)
	protected Boolean failOnError;

	@XmlAttribute(name = "failOnMissing", required = false)
	protected Boolean failOnMissing;

	public List<SettingT> getSettings() {
		if (this.settings == null) {
			this.settings = new ArrayList<>();
		}
		return this.settings;
	}

	public Metadata getMetadata() {
		return this.metadata;
	}

	public void setMetadata(Metadata value) {
		this.metadata = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public boolean isFailOnError() {
		return Tools.coalesce(this.failOnError, Boolean.FALSE);
	}

	public void setFailOnError(Boolean value) {
		this.failOnError = value;
	}

	public boolean isFailOnMissing() {
		return Tools.coalesce(this.failOnMissing, Boolean.FALSE);
	}

	public void setFailOnMissing(Boolean value) {
		this.failOnMissing = value;
	}
}