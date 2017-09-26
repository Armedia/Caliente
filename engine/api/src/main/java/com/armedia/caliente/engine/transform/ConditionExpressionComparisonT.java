
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class ConditionExpressionComparisonT extends ExpressionT implements Condition {

	@XmlAttribute(name = "comparison")
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}

}