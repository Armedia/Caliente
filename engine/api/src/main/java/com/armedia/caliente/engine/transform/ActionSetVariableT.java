
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetVariable.t", propOrder = {
	"name", "type", "value"
})
public class ActionSetVariableT extends ConditionalActionT {

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	@XmlElement(name = "type", required = false)
	protected String type;

	@XmlElement(name = "value", required = true)
	protected ExpressionT value;

	public ExpressionT getName() {
		return this.name;
	}

	public void setName(ExpressionT value) {
		this.name = value;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String value) {
		this.type = value;
	}

	public ExpressionT getValue() {
		return this.value;
	}

	public void setValue(ExpressionT value) {
		this.value = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
	}

}