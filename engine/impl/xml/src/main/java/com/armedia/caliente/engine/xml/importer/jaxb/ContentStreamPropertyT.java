package com.armedia.caliente.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "contentStreamProperty.t", propOrder = {})
public class ContentStreamPropertyT implements Comparable<ContentStreamPropertyT> {

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "value", required = true)
	protected String value;

	public ContentStreamPropertyT() {
		this(null, null);
	}

	public ContentStreamPropertyT(String name, String value) {
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
	public int compareTo(ContentStreamPropertyT o) {
		if (o == null) { return 1; }
		return Tools.compare(this.name, o.name);
	}
}