
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsAttributeRepeating.t")
public class ConditionIsAttributeRepeatingT extends ExpressionT implements Condition {

	@XmlAttribute(name = "comparison")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

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

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
		return false;
	}

}