
package com.armedia.caliente.engine.transform.xml.conditions;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
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
		switch (type) {
			case BOOLEAN:
				if (Boolean.class.isInstance(object)) { return object; }
				return Tools.toBoolean(object);

			case DATETIME:
				if (Date.class.isInstance(object)) { return object; }
				if (Calendar.class.isInstance(object)) { return Calendar.class.cast(object).getTime(); }
				try {
					return new CmfValue(type, object).asTime();
				} catch (ParseException e) {
					throw new TransformationException(
						String.format("Failed to convert the value [%s] as a Date", object), e);
				}

			case DOUBLE:
				if (Double.class.isInstance(object)) { return object; }
				if (Number.class.isInstance(object)) { return Number.class.cast(object).doubleValue(); }
				try {
					return Long.parseLong(object.toString());
				} catch (NumberFormatException e) {
					throw new TransformationException(
						String.format("Failed to convert the value [%s] as an integer", object), e);
				}

			case INTEGER:
				if (Byte.class.isInstance(object)) { return object; }
				if (Short.class.isInstance(object)) { return object; }
				if (Integer.class.isInstance(object)) { return object; }
				if (Long.class.isInstance(object)) { return object; }
				if (Number.class.isInstance(object)) { return Number.class.cast(object).longValue(); }
				try {
					return Long.parseLong(object.toString());
				} catch (NumberFormatException e) {
					throw new TransformationException(
						String.format("Failed to convert the value [%s] as an integer", object), e);
				}

			case URI:
				if (URI.class.isInstance(object)) { return object; }
				try {
					return new URI(object.toString());
				} catch (URISyntaxException e) {
					throw new TransformationException(
						String.format("Failed to convert the value [%s] as a URI", object), e);
				}

			case HTML:
			case ID:
			case STRING:
				return object.toString();

			case BASE64_BINARY:
			case OTHER:
			default:
				return object;
		}
	}

	private Collection<?> getMultiple(Object o) {
		if (o == null) { return null; }
		if (Collection.class.isInstance(o)) { return Collection.class.cast(o); }
		if (o.getClass().isArray()) { return Arrays.asList((Object[]) o); }
		return null;
	}

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		final CmfDataType type = getType();
		Expression leftExp = getLeft();
		Object leftVal = Expression.eval(leftExp, ctx);
		Expression rightExp = getRight();
		Object rightVal = Expression.eval(rightExp, ctx);

		Comparison comparison = getComparison();
		if (comparison == Comparison.EQ) {
			// If we're testing equality, we can actually compare multivalues...
			Collection<?> leftCol = getMultiple(leftVal);
			Collection<?> rightCol = getMultiple(rightVal);
			// If their cardinality is different, we can't compare them...
			if ((leftCol == null) != (rightCol == null)) { return false; }
			// They can be compared directly using equals(), so let's do it!
			if ((leftCol != null) && (rightCol != null)) { return Tools.equals(leftCol, rightCol); }
			// We can't compare directly, so let's fall back to the original comparison...
		}

		return getComparison().check(getType(), castTo(type, leftVal), castTo(type, rightVal));
	}

}