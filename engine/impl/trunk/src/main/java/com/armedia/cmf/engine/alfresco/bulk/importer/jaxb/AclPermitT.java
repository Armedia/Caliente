package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "aclPermit.t")
public class AclPermitT {

	@XmlAttribute(name = "type", required = true)
	protected PermitTypeT type;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "level", required = true)
	protected int level;

	@XmlAttribute(name = "extended", required = false)
	protected String extended;

	public PermitTypeT getType() {
		return this.type;
	}

	public void setType(PermitTypeT value) {
		this.type = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public int getLevel() {
		return this.level;
	}

	public void setLevel(int value) {
		this.level = value;
	}

	public String getExtended() {
		return this.extended;
	}

	public void setExtended(String value) {
		this.extended = value;
	}

	@Override
	public String toString() {
		return String.format("AclPermitT [type=%s, name=%s, level=%s, extended=%s]", this.type, this.name, this.level,
			this.extended);
	}
}