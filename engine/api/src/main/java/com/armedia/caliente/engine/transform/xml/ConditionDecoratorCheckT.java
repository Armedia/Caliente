
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionDecoratorCheck.t", propOrder = {
	"value"
})
public class ConditionDecoratorCheckT implements Condition {

	@XmlValue
	protected String value;

	@XmlAttribute(name = "comparison")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Comparison getComparison() {
		return Comparison.get(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison comparison) {
		this.comparison = (comparison != null ? comparison.name() : null);
	}

	@Override
	public boolean evaluate(TransformationContext ctx) {
		Comparison comparison = getComparison();
		// TODO: How to get the decorators for the object?
		return (comparison != null);
	}

}