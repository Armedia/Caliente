
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Cardinality;
import com.armedia.caliente.engine.dynamic.xml.CardinalityAdapter;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.engine.dynamic.xml.RegularExpression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractReplaceValue extends AbstractTransformValue {

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	@XmlElement(name = "regex", required = true)
	protected RegularExpression regex;

	@XmlElement(name = "replacement", required = false)
	protected Expression replacement;

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

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
	protected final void applyTransformation(DynamicElementContext ctx, DynamicValue candidate) throws ActionException {
		RegularExpression regexBase = getRegex();
		final String regex = Tools.toString(ActionTools.eval(regexBase, ctx));
		if (regex == null) { throw new ActionException("No regular expression given to check against"); }
		final String replacement = Tools.coalesce(Tools.toString(ActionTools.eval(getReplacement(), ctx)), "");

		int flags = 0;
		if (!regexBase.isCaseSensitive()) {
			flags = Pattern.CASE_INSENSITIVE;
		}

		if (!candidate.isRepeating()) {
			// Cardinality is irrelevant...
			Object oldValue = candidate.getValue();
			String oldString = ((oldValue != null) ? Tools.toString(oldValue) : null);
			Object newValue = null;
			if (oldString != null) {
				newValue = Pattern.compile(regex, flags).matcher(oldString).replaceAll(replacement);
			}
			candidate.setValue(newValue);
			return;
		}

		final int valueCount = candidate.getSize();
		if (valueCount > 0) {
			final List<Object> newValues = new LinkedList<>();
			final Cardinality cardinality = getCardinality();
			switch (cardinality) {
				case ALL:
					for (Object oldValue : candidate.getValues()) {
						String oldString = Tools.toString(oldValue);
						String newString = null;
						if (oldString != null) {
							newString = Pattern.compile(regex, flags).matcher(oldString).replaceAll(replacement);
						}
						newValues.add(newString);
					}
					break;

				case FIRST:
				case LAST:
					for (Object oldValue : candidate.getValues()) {
						newValues.add(oldValue);
					}
					int targetIndex = (cardinality == Cardinality.FIRST ? 0 : valueCount - 1);
					String oldString = Tools.toString(newValues.remove(targetIndex));
					String newString = null;
					if (oldString != null) {
						newString = Pattern.compile(regex, flags).matcher(oldString).replaceAll(replacement);
					}
					newValues.add(targetIndex, newString);
					break;
			}
			candidate.setValues(newValues);
		}
	}
}