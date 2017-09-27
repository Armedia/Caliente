
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsCalientePropertyValue.t", propOrder = {
	"name", "value"
})
public class ConditionIsCalientePropertyValueT extends ConditionCheckBaseT {

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	@XmlElement(name = "value", required = true)
	protected ExpressionT value;

	@XmlAttribute(name = "cardinality")
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	public ExpressionT getName() {
		return this.name;
	}

	public void setName(ExpressionT value) {
		this.name = value;
	}

	public ExpressionT getValue() {
		return this.value;
	}

	public void setValue(ExpressionT value) {
		this.value = value;
	}

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		ExpressionT nameExp = getName();
		Object name = (nameExp != null ? nameExp.evaluate(ctx) : null);
		if (name == null) { throw new IllegalStateException("No name was given for the property value check"); }

		CmfProperty<V> property = ctx.getObject().getProperty(name.toString());
		if (property == null) { return false; }

		Comparison comparison = getComparison();
		ExpressionT valueExp = getValue();
		Object value = (valueExp != null ? valueExp.evaluate(ctx) : null);
		if (!property.isRepeating()) {
			// Check the one and only value
			CmfValueCodec<V> codec = ctx.getObject().getTranslator().getCodec(property.getType());
			V v = property.getValue();
			if (v == null) {
				v = codec.getNull();
			}
			CmfValue cv = codec.encodeValue(v);
			if ((cv == null) || cv.isNull()) {
				return comparison.check(null, Tools.toString(value));
			} else {
				return comparison.check(cv.asString(), Tools.toString(value));
			}
		}

		switch (getCardinality()) {
			case ALL:
				// Check against all attribute values, until one succeeds
				break;
			case FIRST:
				// Only check the first attribute value
				break;
			case LAST:
				// Only check the last attribute value
				break;
		}
		return false;
	}

}