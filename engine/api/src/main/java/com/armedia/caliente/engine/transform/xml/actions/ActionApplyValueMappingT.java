
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Cardinality;
import com.armedia.caliente.engine.transform.xml.CardinalityAdapter;
import com.armedia.caliente.engine.transform.xml.ConditionalActionT;
import com.armedia.caliente.engine.transform.xml.ExpressionT;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionApplyValueMapping.t", propOrder = {
	"type", "name", "attribute", "cardinality"
})
public class ActionApplyValueMappingT extends ConditionalActionT {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfTypeAdapter.class)
	protected CmfType type;

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	@XmlElement(name = "attribute", required = false)
	protected ExpressionT attribute;

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	public void setType(CmfType type) {
		this.type = type;
	}

	public CmfType getType() {
		return this.type;
	}

	public ExpressionT getName() {
		return this.name;
	}

	public void setName(ExpressionT name) {
		this.name = name;
	}

	public ExpressionT getAttribute() {
		return this.attribute;
	}

	public void setAttribute(ExpressionT attribute) {
		this.attribute = attribute;
	}

	public Cardinality getCardinality() {
		return this.cardinality;
	}

	public void setCardinality(Cardinality cardinality) {
		this.cardinality = cardinality;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext ctx) {
		// TODO Implement this transformation
	}
}