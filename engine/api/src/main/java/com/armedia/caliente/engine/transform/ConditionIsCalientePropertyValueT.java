
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsCalientePropertyValue.t", propOrder = {
	"name", "value"
})
public class ConditionIsCalientePropertyValueT implements Condition {

	@XmlElement(required = true)
	protected ExpressionT name;
	@XmlElement(required = true)
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

	public String getComparison() {
		if (this.comparison == null) {
			return "eq";
		} else {
			return this.comparison;
		}
	}

	public void setComparison(String value) {
		this.comparison = value;
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