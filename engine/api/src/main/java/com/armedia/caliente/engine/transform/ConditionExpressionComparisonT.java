
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlTransient
public abstract class ConditionExpressionComparisonT extends ExpressionT implements Condition {

	@XmlAttribute(name = "comparison")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	public Comparison getComparison() {
		return Comparison.get(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison value) {
		this.comparison = (value != null ? value.name() : null);
	}

}