
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionAddDecorator.t", propOrder = {
	"decorator"
})
public class ActionAddDecoratorT extends ConditionalActionT {

	@XmlElement(name = "decorator", required = true)
	protected ExpressionT decorator;

	public ExpressionT getDecorator() {
		return this.decorator;
	}

	public void setDecorator(ExpressionT value) {
		this.decorator = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO implement this transformation
	}

}