
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsVariableValue.t", propOrder = {
	"name", "value"
})
public class ConditionIsVariableValueT implements Condition {

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	@XmlElement(name = "value", required = true)
	protected ExpressionT value;

	@XmlAttribute(name = "comparison")
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

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

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO implement this condition
		return false;
	}

}