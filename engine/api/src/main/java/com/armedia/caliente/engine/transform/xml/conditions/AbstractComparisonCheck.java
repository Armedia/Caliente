
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.ComparisonAdapter;
import com.armedia.caliente.engine.transform.xml.Condition;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractComparisonCheck implements Condition {

	@XmlAttribute(name = "comparison")
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	public final Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public final void setComparison(Comparison value) {
		this.comparison = value;
	}

}