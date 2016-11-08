package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "types")
@XmlType(name = "types.t", propOrder = {
	"type"
})
public class TypesT extends AggregatorBase<TypeT> {

	public TypesT() {
		super("type");
	}

	@XmlElement(name = "type")
	public List<TypeT> getType() {
		return getItems();
	}
}