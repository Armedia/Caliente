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

	@XmlAttribute(name = "repeating", required = false)
	protected boolean repeating = false;

	@XmlAttribute(name = "inherited", required = true)
	protected boolean inherited = false;

	@XmlAttribute(name = "sourceName", required = true)
	protected String sourceName;

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

	public boolean isInherited() {
		return this.inherited;
	}

	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	@Override
	public String toString() {
		return String.format(
			"AttributeDefT [name=%s, dataType=%s, length=%s, repeating=%s, inherited=%s, sourceName=%s]", this.name,
			this.dataType, this.length, this.repeating, this.inherited);
	}
}