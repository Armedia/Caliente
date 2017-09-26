
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceSubtype.t", propOrder = {
	"regex", "replacement"
})
public class ActionReplaceSubtypeT extends ConditionalActionT {

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
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO implement this transformation
	}

}