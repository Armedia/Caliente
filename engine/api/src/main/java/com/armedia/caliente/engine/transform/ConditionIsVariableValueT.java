
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	@XmlAttribute(name = "cardinality")
	protected CardinalityT cardinality;

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
		return Comparison.get(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison value) {
		this.comparison = (value != null ? value.name() : null);
	}

	public CardinalityT getCardinality() {
		if (this.cardinality == null) {
			return CardinalityT.ANY;
		} else {
			return this.cardinality;
		}
	}

	public void setCardinality(CardinalityT value) {
		this.cardinality = value;
	}

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
		return false;
	}

}