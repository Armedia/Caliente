
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.ObjectDataMember;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Cardinality;
import com.armedia.caliente.engine.transform.xml.CardinalityAdapter;
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractAttributeCalientePropertyVariableValueCheck extends AbstractComparisonCheck {

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "value", required = true)
	protected Expression value;

	@XmlAttribute(name = "cardinality")
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression value) {
		this.name = value;
	}

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

	protected abstract ObjectDataMember getCandidate(TransformationContext ctx, String name);

	protected abstract Object getCandidateValue(ObjectDataMember candidate, int pos);

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		Expression nameExp = getName();
		Object name = Expression.eval(nameExp, ctx);
		if (name == null) { throw new TransformationException("No name was given for the candidate value check"); }

		ObjectDataMember candidate = getCandidate(ctx, name.toString());
		if (candidate == null) { return false; }

		Comparison comparison = getComparison();
		Expression valueExp = getValue();
		Object comparand = Expression.eval(valueExp, ctx);
		if (comparand == null) { throw new TransformationException(
			"No comparand value given to check the name against"); }
		if (!candidate.isRepeating()) {
			// Check the one and only value
			Object cv = getCandidateValue(candidate, 0);
			if (cv == null) {
				return comparison.check(candidate.getType(), null, comparand);
			} else {
				return comparison.check(candidate.getType(), cv, comparand);
			}
		}

		final int valueCount = candidate.getSize();
		if (valueCount > 0) {
			final CmfDataType type = candidate.getType();
			switch (getCardinality()) {
				case ALL:
					// Check against all attribute values, until one succeeds
					for (int i = 0; i < valueCount; i++) {
						if (comparison.check(type, getCandidateValue(candidate, i), comparand)) { return true; }
					}
					break;

				case FIRST:
					// Only check the first attribute value
					return comparison.check(type, getCandidateValue(candidate, 0), comparand);

				case LAST:
					// Only check the last attribute value
					return comparison.check(type, getCandidateValue(candidate, valueCount - 1), comparand);
			}
		}
		return false;
	}

}