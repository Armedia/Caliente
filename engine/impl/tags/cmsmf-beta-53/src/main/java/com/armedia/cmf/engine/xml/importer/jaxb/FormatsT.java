package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "formats")
@XmlType(name = "formats.t", propOrder = {
	"format"
})
public class FormatsT extends AggregatorBase<FormatT> {

	public FormatsT() {
		super("format");
	}

	@XmlElement(name = "format")
	public List<FormatT> getFormat() {
		return getItems();
	}
}