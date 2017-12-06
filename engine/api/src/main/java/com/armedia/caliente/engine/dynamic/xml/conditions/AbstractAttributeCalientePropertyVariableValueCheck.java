
package com.armedia.caliente.engine.dynamic.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Cardinality;
import com.armedia.caliente.engine.dynamic.xml.CardinalityAdapter;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.Expression;
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

	protected abstract Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx);

	@Override
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		Expression nameExp = getName();
		Object name = ConditionTools.eval(nameExp, ctx);
		if (name == null) { throw new ConditionException("No name was given for the candidate value check"); }

		final Map<String, DynamicValue> values = getCandidateValues(ctx);

		DynamicValue candidate = values.get(name.toString());
		if (candidate == null) { return false; }

		Comparison comparison = getComparison();
		Expression valueExp = getValue();
		Object comparand = ConditionTools.eval(valueExp, ctx);
		if (comparand == null) { throw new ConditionException("No comparand value given to check the name against"); }
		if (!candidate.isRepeating()) {
			// Check the one and only value
			Object cv = candidate.getValue();
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
						if (comparison.check(type, candidate.getValues().get(i), comparand)) { return true; }
					}
					break;

				case FIRST:
					// Only check the first attribute value
					return comparison.check(type, candidate.getValues().get(0), comparand);

				case LAST:
					// Only check the last attribute value
					return comparison.check(type, candidate.getValues().get(valueCount - 1), comparand);
			}
		}
		return false;
	}

}