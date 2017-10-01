
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Cardinality;
import com.armedia.caliente.engine.transform.xml.CardinalityAdapter;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractReplaceValue extends AbstractTransformValue {

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	@XmlElement(name = "regex", required = true)
	protected Expression regex;

	@XmlElement(name = "replacement", required = true)
	protected Expression replacement;

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

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
	protected final void applyTransformation(TransformationContext ctx, TypedValue candidate)
		throws TransformationException {
		final String regex = Tools.toString(Expression.eval(getRegex(), ctx));
		if (regex == null) { throw new TransformationException("No regular expression given to check against"); }
		final String replacement = Tools.toString(Expression.eval(getReplacement(), ctx));
		if (replacement == null) { throw new TransformationException("No replacement given to apply"); }

		if (!candidate.isRepeating()) {
			// Cardinality is irrelevant...
			Object oldValue = candidate.getValue();
			String oldString = ((oldValue != null) ? Tools.toString(oldValue) : null);
			Object newValue = null;
			if (oldString != null) {
				newValue = oldString.replaceAll(regex, replacement);
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
							newString = oldString.replaceAll(regex, replacement);
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
						newString = oldString.replaceAll(regex, replacement);
					}
					newValues.add(targetIndex, newString);
					break;
			}
			candidate.setValues(newValues);

		}
	}
}