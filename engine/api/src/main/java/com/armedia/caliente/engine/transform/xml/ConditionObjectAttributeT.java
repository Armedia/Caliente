
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.store.CmfAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionObjectAttribute.t", propOrder = {
	"name", "comparison", "value"
})
public class ConditionObjectAttributeT implements Condition {

	@XmlElement(required = true)
	protected String name;

	@XmlElement(required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	@XmlElement(required = true)
	protected ExpressionT value;

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
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
	public boolean evaluate(TransformationContext ctx) {
		String name = getName();
		CmfAttribute<?> att = ctx.getObject().getAttribute(name);
		ExpressionT valueExp = getValue();
		String value = (valueExp != null ? valueExp.evaluate(ctx) : "");

		Comparison comparison = getComparison();
		for (Object attVal : att) {
			String attStr = attVal.toString(); // TODO: Fix to use a codec
			if (comparison.eval(value, attStr)) { return true; }
		}

		return false;
	}

}