
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetAttribute.t", propOrder = {
	"name", "type", "value"
})
public class ActionSetAttributeT extends ConditionalActionT {

	@XmlElement(required = true)
	protected ExpressionT name;

	@XmlElement(required = false)
	protected String type;

	@XmlElement(required = true)
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