package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "types.t", propOrder = {
	"type"
})
public class TypesT {

	@XmlElement(name = "type", required = false)
	protected List<TypeDefT> type;

	public List<TypeDefT> getType() {
		if (this.type == null) {
			this.type = new ArrayList<TypeDefT>();
		}
		return this.type;
	}

}