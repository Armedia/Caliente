
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.Condition;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCheckExpression.t", propOrder = {
	"left", "right"
})
public class ConditionCheckExpressionT implements Condition {

	@XmlElement(required = true)
	protected ExpressionT left;

	@XmlElement(required = true)
	protected ExpressionT right;

	@XmlAttribute(name = "comparison")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	public ExpressionT getLeft() {
		return this.left;
	}

	public void setLeft(ExpressionT value) {
		this.left = value;
	}

	public ExpressionT getRight() {
		return this.right;
	}

	public void setRight(ExpressionT value) {
		this.right = value;
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

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
		return false;
	}

}