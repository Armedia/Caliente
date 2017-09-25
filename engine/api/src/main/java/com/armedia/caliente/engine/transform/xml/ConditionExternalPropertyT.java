
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionExternalProperty.t", propOrder = {
	"name", "comparison", "value"
})
public class ConditionExternalPropertyT implements Condition {

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
	public <V> boolean evaluate(TransformationContext<V> ctx) {
		String name = getName();
		CmfProperty<V> prop = ctx.getObject().getProperty(name);
		ExpressionT valueExp = getValue();
		String value = (valueExp != null ? valueExp.evaluate(ctx) : "");
		Comparison comparison = getComparison();
		CmfValueCodec<V> codec = ctx.getCodec(prop.getType());
		for (V propVal : prop) {
			CmfValue v = codec.encodeValue(propVal);
			if (comparison.eval(value, v.asString())) { return true; }
		}

		return false;
	}
}