
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionMapAttributeValue.t", propOrder = {
	"attributeName", "cardinality", "map"
})
public class ActionMapAttributeValueT extends ConditionalActionT {

	@XmlElement(name = "attribute-name", required = true)
	protected ExpressionT attributeName;

	@XmlElement(required = false)
	protected CardinalityT cardinality;

	@XmlElement(required = true)
	protected MapValueT map;

	public ExpressionT getAttributeName() {
		return this.attributeName;
	}

	public void setAttributeName(ExpressionT value) {
		this.attributeName = value;
	}

	public CardinalityT getCardinality() {
		return this.cardinality;
	}

	public void setCardinality(CardinalityT value) {
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
		// TODO Auto-generated method stub

	}
}