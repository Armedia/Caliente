package com.armedia.caliente.engine.dynamic.xml.metadata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataNameMapping.t", propOrder = {
	"from", "to"
})
public class MetadataNameMapping {

	@XmlElement(name = "from", required = true)
	protected String from;

	@XmlElement(name = "to", required = true)
	protected Expression to;

	public String getFrom() {
		return this.from;
	}

	public void setFrom(String value) {
		this.from = value;
	}

	public Expression getTo() {
		return this.to;
	}

	public void setTo(Expression value) {
		this.to = value;
	}

}
