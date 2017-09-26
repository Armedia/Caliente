
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionCustomAction.t", propOrder = {
	"className"
})
public class ActionCustomActionT extends ConditionalActionT {

	@XmlElement(name = "class-name", required = true)
	protected ExpressionT className;

	public ExpressionT getClassName() {
		return this.className;
	}

	public void setClassName(ExpressionT value) {
		this.className = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
	}

}