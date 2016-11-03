package com.armedia.cmf.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "contentInfoProperty.t", propOrder = {})
public class ContentInfoPropertyT implements Comparable<ContentInfoPropertyT> {

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "value", required = true)
	protected String value;

	public ContentInfoPropertyT() {
		this(null, null);
	}

	public ContentInfoPropertyT(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int compareTo(ContentInfoPropertyT o) {
		if (o == null) { return 1; }
		return Tools.compare(this.name, o.name);
	}
}