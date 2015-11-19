package com.armedia.cmf.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeDef.t")
public class AttributeDefT extends AttributeBaseT {

	@XmlAttribute(name = "length", required = false)
	protected int length = 0;

	@XmlAttribute(name = " repeating", required = false)
	protected boolean repeating = false;

	public int getLength() {
		return this.length;
	}

	public void setLength(int value) {
		this.length = value;
	}

	public boolean isRepeating() {
		return this.repeating;
	}

	public void setRepeating(boolean value) {
		this.repeating = value;
	}
}