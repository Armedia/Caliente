package com.armedia.caliente.engine.xml.extmeta;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataAttributeQuery.t", propOrder = {
	"sql", "valueColumn", "typeColumn", "multivaluedColumn"
})
public class MetadataAttributeQuery {

	@XmlElement(name = "sql", required = true)
	protected String sql;

	@XmlElement(name = "value-column", required = true)
	protected String valueColumn;

	@XmlElement(name = "type-column", required = true)
	protected String typeColumn;

	@XmlElement(name = "multivalued-column", required = true)
	protected String multivaluedColumn;

}