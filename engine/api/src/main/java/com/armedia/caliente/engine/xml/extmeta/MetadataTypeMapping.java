package com.armedia.caliente.engine.xml.extmeta;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.xml.CmfDataTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataTypeMapping.t", propOrder = {
	"name", "type"
})
public class MetadataTypeMapping {

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "type", required = true)
	@XmlJavaTypeAdapter(CmfDataTypeAdapter.class)
	protected CmfDataType type;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CmfDataType getType() {
		return this.type;
	}

	public void setType(CmfDataType type) {
		this.type = type;
	}
}