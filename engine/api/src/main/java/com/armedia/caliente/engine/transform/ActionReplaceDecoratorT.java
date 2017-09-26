
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceDecorator.t", propOrder = {
	"regex", "replacement"
})
public class ActionReplaceDecoratorT extends ConditionalActionT {

	@XmlElement(required = true)
	protected ExpressionT regex;

	@XmlElement(required = true)
	protected ExpressionT replacement;

	public ExpressionT getRegex() {
		return this.regex;
	}

	public void setRegex(ExpressionT value) {
		this.regex = value;
	}

	public ExpressionT getReplacement() {
		return this.replacement;
	}

	public void setReplacement(ExpressionT value) {
		this.replacement = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
	}

}