
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.ConditionalActionT;
import com.armedia.caliente.engine.transform.xml.ExpressionT;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionAddDecorator.t", propOrder = {
	"decorator"
})
public class AddDecorator extends ConditionalActionT {

	@XmlElement(name = "decorator", required = true)
	protected ExpressionT decorator;

	public ExpressionT getDecorator() {
		return this.decorator;
	}

	public void setDecorator(ExpressionT value) {
		this.decorator = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation
	}

}