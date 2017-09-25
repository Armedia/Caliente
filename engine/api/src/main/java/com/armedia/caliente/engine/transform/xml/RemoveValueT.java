
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "removeValue.t", propOrder = {
	"name"
})
public class RemoveValueT implements Transformation {

	@XmlElement(required = true)
	protected ExpressionT name;

	public ExpressionT getName() {
		return this.name;
	}

	public void setName(ExpressionT value) {
		this.name = value;
	}

	@Override
	public <V> void apply(TransformationContext<V> ctx) {
		ExpressionT name = getName();
		if (name == null) { return; }
		ctx.getObject().removeAttribute(name.evaluate(ctx));
	}

}