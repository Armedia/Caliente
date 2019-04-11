package com.armedia.caliente.cli.datagen.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rendition.t")
public class Rendition extends Content {

	@XmlAttribute(name = "id", required = true)
	protected String id;

	public String getId() {
		return this.id;
	}

	public void setId(String value) {
		this.id = value;
	}
}