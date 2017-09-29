
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceDecorator.t", propOrder = {
	"regex", "replacement"
})
public class DecoratorReplace extends ConditionalAction {

	@XmlElement(name = "regex", required = true)
	protected Expression regex;

	@XmlElement(name = "replacement", required = true)
	protected Expression replacement;

	public Expression getRegex() {
		return this.regex;
	}

	public void setRegex(Expression value) {
		this.regex = value;
	}

	public Expression getReplacement() {
		return this.replacement;
	}

	public void setReplacement(Expression value) {
		this.replacement = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation
	}

}