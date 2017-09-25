
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionExpression.t", propOrder = {
	"source", "comparison", "value"
})
public class ConditionExpressionT implements Condition {

	@XmlElement(required = true)
	protected ExpressionT source;

	@XmlElement(required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	@XmlElement(required = true)
	protected ExpressionT value;

	public ExpressionT getSource() {
		return this.source;
	}

	public void setSource(ExpressionT value) {
		this.source = value;
	}

	public Comparison getComparison() {
		return Comparison.get(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison comparison) {
		this.comparison = (comparison != null ? comparison.name() : null);
	}

	public ExpressionT getValue() {
		return this.value;
	}

	public void setValue(ExpressionT value) {
		this.value = value;
	}

	@Override
	public <V> boolean evaluate(TransformationContext<V> ctx) {
		ExpressionT sourceExp = getSource();
		String source = (sourceExp != null ? sourceExp.evaluate(ctx) : "");
		ExpressionT valueExp = getValue();
		String value = (valueExp != null ? valueExp.evaluate(ctx) : "");
		return getComparison().eval(source, value);
	}

}