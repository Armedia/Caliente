
package com.armedia.caliente.engine.xml.actions;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.xml.ConditionalAction;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.engine.xml.RegularExpression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractSingleReplace extends ConditionalAction {

	@XmlElement(name = "regex", required = true)
	protected RegularExpression regex;

	@XmlElement(name = "replacement", required = false)
	protected Expression replacement;

	public RegularExpression getRegex() {
		return this.regex;
	}

	public void setRegex(RegularExpression regex) {
		this.regex = regex;
	}

	public Expression getReplacement() {
		return this.replacement;
	}

	public void setReplacement(Expression replacement) {
		this.replacement = replacement;
	}

	protected abstract String getOldValue(TransformationContext ctx);

	protected abstract void setNewValue(TransformationContext ctx, String newValue);

	@Override
	protected final void applyTransformation(TransformationContext ctx) throws TransformationException {
		final RegularExpression regexBase = getRegex();
		final String regex = Tools.toString(Expression.eval(getRegex(), ctx));
		if (regex == null) { throw new TransformationException("No regular expression given to check against"); }
		final String replacement = Tools.coalesce(Tools.toString(Expression.eval(getReplacement(), ctx)), "");
		String oldValue = getOldValue(ctx);
		int flags = 0;
		if (!regexBase.isCaseSensitive()) {
			flags |= Pattern.CASE_INSENSITIVE;
		}
		setNewValue(ctx, Pattern.compile(regex, flags).matcher(oldValue).replaceAll(replacement));
	}

}