
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.engine.dynamic.xml.RegularExpression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceSecondarySubtype.t", propOrder = {
	"regex", "replacement"
})
public class SecondarySubtypeReplace extends ConditionalAction {

	@XmlElement(name = "regex", required = true)
	protected RegularExpression regex;

	@XmlElement(name = "replacement", required = false)
	protected Expression replacement;

	public RegularExpression getRegex() {
		return this.regex;
	}

	public void setRegex(RegularExpression value) {
		this.regex = value;
	}

	public Expression getReplacement() {
		return this.replacement;
	}

	public void setReplacement(Expression value) {
		this.replacement = value;
	}

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		Set<String> currentSecondaries = ctx.getDynamicObject().getSecondarySubtypes();
		if (currentSecondaries.isEmpty()) { return; }

		RegularExpression regexBase = getRegex();
		final String regex = Tools.toString(ActionTools.eval(regexBase, ctx));
		if (regex == null) { throw new ActionException("No regular expression given to check against"); }
		final String replacement = Tools.coalesce(Tools.toString(ActionTools.eval(getReplacement(), ctx)), "");

		int flags = 0;
		if (!regexBase.isCaseSensitive()) {
			flags |= Pattern.CASE_INSENSITIVE;
		}

		Set<String> secondaries = new LinkedHashSet<>(currentSecondaries);
		currentSecondaries.clear();
		for (String s : secondaries) {
			s = Pattern.compile(regex, flags).matcher(s).replaceAll(replacement);
			s = StringUtils.strip(s);
			if (!StringUtils.isEmpty(s)) {
				currentSecondaries.add(s);
			}
		}
	}

}