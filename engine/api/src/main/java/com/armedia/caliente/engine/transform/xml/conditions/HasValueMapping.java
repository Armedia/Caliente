
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Condition;
import com.armedia.caliente.engine.transform.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class HasValueMapping implements Condition {

	@XmlElement(name = "type", required = false)
	protected String type;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "name", required = false)
	protected Expression from;

	@XmlElement(name = "name", required = false)
	protected Expression to;

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression name) {
		this.name = name;
	}

	public Expression getFrom() {
		return this.from;
	}

	public void setFrom(Expression from) {
		this.from = from;
	}

	public Expression getTo() {
		return this.to;
	}

	public void setTo(Expression to) {
		this.to = to;
	}

	@Override
	public boolean check(TransformationContext ctx) {
		// TODO Implement this condition
		return false;
	}
}