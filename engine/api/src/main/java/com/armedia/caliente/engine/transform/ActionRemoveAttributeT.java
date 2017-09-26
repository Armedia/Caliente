
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveAttribute.t", propOrder = {
	"comparison", "attributeName"
})
public class ActionRemoveAttributeT extends ConditionalActionT {

	@XmlElement(name = "comparison", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	@XmlElement(name = "attribute-name", required = true)
	protected ExpressionT attributeName;

	public String getComparison() {
		return this.comparison;
	}

	public void setComparison(String value) {
		this.comparison = value;
	}

	public ExpressionT getAttributeName() {
		return this.attributeName;
	}

	public void setAttributeName(ExpressionT value) {
		this.attributeName = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub

	}

}