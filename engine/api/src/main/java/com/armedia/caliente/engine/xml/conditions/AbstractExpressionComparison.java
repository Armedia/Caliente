
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.xml.Comparison;
import com.armedia.caliente.engine.xml.ComparisonAdapter;
import com.armedia.caliente.engine.xml.Condition;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractExpressionComparison extends Expression implements Condition {

	@XmlAttribute(name = "comparison")
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}
}