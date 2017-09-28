
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class ConditionAttributeCalientePropertyVariableValueCheckT<T extends CmfProperty<CmfValue>>
	extends ConditionCheckBaseT {

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	@XmlElement(name = "value", required = true)
	protected ExpressionT value;

	@XmlAttribute(name = "cardinality")
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	public ExpressionT getName() {
		return this.name;
	}

	public void setName(ExpressionT value) {
		this.name = value;
	}

	public ExpressionT getValue() {
		return this.value;
	}

	public void setValue(ExpressionT value) {
		this.value = value;
	}

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

	protected abstract T getCandidate(TransformationContext ctx, String name);

	@Override
	public boolean check(TransformationContext ctx) {
		ExpressionT nameExp = getName();
		Object name = (nameExp != null ? nameExp.evaluate(ctx) : null);
		if (name == null) { throw new IllegalStateException("No name was given for the candidate value check"); }

		T candidate = getCandidate(ctx, name.toString());
		if (candidate == null) { return false; }

		Comparison comparison = getComparison();
		ExpressionT valueExp = getValue();
		Object comparand = (valueExp != null ? valueExp.evaluate(ctx) : null);
		if (!candidate.isRepeating()) {
			// Check the one and only value
			CmfValue cv = candidate.getValue();
			if ((cv == null) || cv.isNull()) {
				return comparison.check(candidate.getType(), null, comparand);
			} else {
				return comparison.check(candidate.getType(), cv, comparand);
			}
		}

		if (candidate.hasValues()) {
			final int valueCount = candidate.getValueCount();
			switch (getCardinality()) {
				case ALL:
					// Check against all attribute values, until one succeeds
					for (CmfValue v : candidate) {
						if (comparison.check(candidate.getType(), v, comparand)) { return true; }
					}
					break;

				case FIRST:
					// Only check the first attribute value
					return comparison.check(candidate.getType(), candidate.getValue(), comparand);

				case LAST:
					// Only check the last attribute value
					return comparison.check(candidate.getType(), candidate.getValue(valueCount - 1), comparand);
			}
		}
		return false;
	}

}