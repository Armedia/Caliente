
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCheckExpression.t", propOrder = {
	"left", "right"
})
public class ConditionCheckExpressionT extends ConditionCheckBaseT {

	@XmlElement(required = true)
	protected ExpressionT left;

	@XmlElement(required = true)
	protected ExpressionT right;

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

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		ExpressionT leftExp = getLeft();
		Object leftVal = (leftExp != null ? leftExp.evaluate(ctx) : null);
		ExpressionT rightExp = getRight();
		Object rightVal = (rightExp != null ? rightExp.evaluate(ctx) : null);
		return getComparison().check(Tools.toString(leftVal), Tools.toString(rightVal));
	}

}