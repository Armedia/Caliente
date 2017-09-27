
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasAttributeValue.t", propOrder = {
	"name", "value"
})
public class ConditionIsAttributeValueT extends ConditionCheckBaseT {

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

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO implement this condition
		return false;
	}

}