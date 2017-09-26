
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.commons.utilities.Tools;

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

	public Comparison getComparison() {
		return Comparison.get(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison value) {
		this.comparison = (value != null ? value.name() : null);
	}

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		ExpressionT leftExp = getLeft();
		Object leftVal = (leftExp != null ? leftExp.evaluate(ctx) : null);
		ExpressionT rightExp = getRight();
		Object rightVal = (rightExp != null ? rightExp.evaluate(ctx) : null);
		return getComparison().check(Tools.toString(leftVal), Tools.toString(rightVal));
	}

}