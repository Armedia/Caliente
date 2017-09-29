
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceSubtype.t", propOrder = {
	"regex", "replacement"
})
public class SubtypeReplace extends ConditionalAction {

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
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		final String regex = Tools.toString(Expression.eval(getRegex(), ctx));
		if (regex == null) { throw new TransformationException("No regular expression given to check against"); }
		final String replacement = Tools.toString(Expression.eval(getReplacement(), ctx));
		if (replacement == null) { throw new TransformationException("No replacement given to apply"); }
		String oldSubtype = ctx.getSubtype();
		String newSubtype = oldSubtype.replaceAll(regex, replacement);
		ctx.setSubtype(newSubtype);
	}

}