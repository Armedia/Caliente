
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.commons.utilities.Tools;

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
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		Set<String> originalDecorators = ctx.getObject().getDecorators();
		if (originalDecorators.isEmpty()) { return; }

		final String regex = Tools.toString(Expression.eval(getRegex(), ctx));
		if (regex == null) { throw new TransformationException("No regular expression given to check against"); }
		final String replacement = Tools.coalesce(Tools.toString(Expression.eval(getReplacement(), ctx)), "");

		Set<String> decorators = new LinkedHashSet<>(originalDecorators);
		Set<String> newDecorators = new LinkedHashSet<>();
		originalDecorators.clear();
		for (String d : decorators) {
			d = d.replaceAll(regex, replacement);
			d = StringUtils.strip(d);
			if (!StringUtils.isEmpty(d)) {
				newDecorators.add(d);
			}
		}
		originalDecorators.addAll(newDecorators);
	}

}