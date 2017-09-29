
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionAddDecorator.t", propOrder = {
	"decorator"
})
public class DecoratorAdd extends ConditionalAction {

	@XmlElement(name = "decorator", required = true)
	protected Expression decorator;

	public Expression getDecorator() {
		return this.decorator;
	}

	public void setDecorator(Expression value) {
		this.decorator = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation
	}

}