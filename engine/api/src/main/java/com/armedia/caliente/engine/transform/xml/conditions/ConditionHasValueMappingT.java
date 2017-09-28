
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Condition;
import com.armedia.caliente.engine.transform.xml.ExpressionT;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class ConditionHasValueMappingT implements Condition {

	@XmlElement(name = "type", required = false)
	protected String type;

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	@XmlElement(name = "name", required = false)
	protected ExpressionT from;

	@XmlElement(name = "name", required = false)
	protected ExpressionT to;

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public ExpressionT getName() {
		return this.name;
	}

	public void setName(ExpressionT name) {
		this.name = name;
	}

	public ExpressionT getFrom() {
		return this.from;
	}

	public void setFrom(ExpressionT from) {
		this.from = from;
	}

	public ExpressionT getTo() {
		return this.to;
	}

	public void setTo(ExpressionT to) {
		this.to = to;
	}

	@Override
	public boolean check(TransformationContext ctx) {
		// TODO Implement this condition
		return false;
	}
}