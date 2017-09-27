
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceDecorator.t", propOrder = {
	"regex", "replacement"
})
public class ActionReplaceDecoratorT extends ConditionalActionT {

	@XmlElement(name = "regex", required = true)
	protected ExpressionT regex;

	@XmlElement(name = "replacement", required = true)
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
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation
	}

}