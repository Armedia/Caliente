package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attribute.t", propOrder = {
	"value"
})
public class AttributeT extends AttributeBaseT {

	@XmlList
	protected List<String> value;

	public List<String> getValue() {
		if (this.value == null) {
			this.value = new ArrayList<String>();
		}
		return this.value;
	}
}