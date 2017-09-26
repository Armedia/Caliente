
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionAddDecorator.t", propOrder = {
	"decorator"
})
public class ActionAddDecoratorT extends ConditionalActionT {

	@XmlElement(required = true)
	protected ExpressionT decorator;

	/**
	 * Gets the value of the decorator property.
	 *
	 * @return possible object is {@link ExpressionT }
	 *
	 */
	public ExpressionT getDecorator() {
		return this.decorator;
	}

	/**
	 * Sets the value of the decorator property.
	 *
	 * @param value
	 *            allowed object is {@link ExpressionT }
	 *
	 */
	public void setDecorator(ExpressionT value) {
		this.decorator = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
	}

}