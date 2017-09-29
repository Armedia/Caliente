
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Cardinality;
import com.armedia.caliente.engine.transform.xml.CardinalityAdapter;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionMapAttributeValue.t", propOrder = {
	"name", "cardinality", "map"
})
public class MapAttributeValue extends ConditionalAction {

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	@XmlElement(name = "map", required = true)
	protected MapValue map;

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression value) {
		this.name = value;
	}

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

	public MapValue getMap() {
		return this.map;
	}

	public void setMap(MapValue value) {
		this.map = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation

	}
}