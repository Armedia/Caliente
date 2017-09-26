
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionMapAttributeValue.t", propOrder = {
	"attributeName", "cardinality", "map"
})
public class ActionMapAttributeValueT extends ConditionalActionT {

	@XmlElement(name = "attribute-name", required = true)
	protected ExpressionT attributeName;

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	@XmlElement(name = "map", required = true)
	protected MapValueT map;

	public ExpressionT getAttributeName() {
		return this.attributeName;
	}

	public void setAttributeName(ExpressionT value) {
		this.attributeName = value;
	}

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

	public MapValueT getMap() {
		return this.map;
	}

	public void setMap(MapValueT value) {
		this.map = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO implement this transformation

	}
}