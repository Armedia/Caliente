
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCheckExpression.t", propOrder = {
	"left", "right"
})
public class CheckExpression extends AbstractComparisonCheck {

	@XmlElement(required = true)
	protected Expression left;

	@XmlElement(required = true)
	protected Expression right;

	public Expression getLeft() {
		return this.left;
	}

	public void setLeft(Expression value) {
		this.left = value;
	}

	public Expression getRight() {
		return this.right;
	}

	public void setRight(Expression value) {
		this.right = value;
	}

	@Override
	public boolean check(TransformationContext ctx) {
		Expression leftExp = getLeft();
		Object leftVal = (leftExp != null ? leftExp.evaluate(ctx) : null);
		Expression rightExp = getRight();
		Object rightVal = (rightExp != null ? rightExp.evaluate(ctx) : null);
		// TODO: What data types are expected for the expressions? Should we add an
		// element/attribute to specify?
		return getComparison().check(null, leftVal, rightVal);
	}

}