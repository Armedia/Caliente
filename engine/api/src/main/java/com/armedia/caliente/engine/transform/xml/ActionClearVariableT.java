
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionClearVariable.t", propOrder = {
	"name"
})
public class ActionClearVariableT extends ConditionalActionT {

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	public ExpressionT getName() {
		return this.name;
	}

	public void setName(ExpressionT value) {
		this.name = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation
	}

}