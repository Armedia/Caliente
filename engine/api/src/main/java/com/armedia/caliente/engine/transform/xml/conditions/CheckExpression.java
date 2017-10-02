
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.xml.CmfDataTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCheckExpression.t", propOrder = {
	"left", "right", "type"
})
public class CheckExpression extends AbstractComparisonCheck {

	@XmlElement(name = "left", required = true)
	protected Expression left;

	@XmlElement(name = "right", required = true)
	protected Expression right;

	@XmlAttribute(name = "type")
	@XmlJavaTypeAdapter(CmfDataTypeAdapter.class)
	protected CmfDataType type;

	public final CmfDataType getType() {
		return Tools.coalesce(this.type, CmfDataType.STRING);
	}

	public final void setType(CmfDataType type) {
		this.type = type;
	}

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

	private Object castTo(CmfDataType type, Object object) throws TransformationException {
		if (object == null) { return null; }
		// TODO: Make sure we typecast the object properly...
		return object;
	}

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		final CmfDataType type = getType();
		Expression leftExp = getLeft();
		Expression rightExp = getRight();
		return getComparison().check(getType(), castTo(type, Expression.eval(leftExp, ctx)),
			castTo(type, Expression.eval(rightExp, ctx)));
	}

}