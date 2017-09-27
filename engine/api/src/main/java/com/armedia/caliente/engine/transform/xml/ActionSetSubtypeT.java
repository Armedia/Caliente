
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetSubtype.t", propOrder = {
	"subtype"
})
public class ActionSetSubtypeT extends ConditionalActionT {

	@XmlElement(name = "subtype", required = true)
	protected ExpressionT subtype;

	public ExpressionT getSubtype() {
		return this.subtype;
	}

	public void setSubtype(ExpressionT value) {
		this.subtype = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		// TODO implement this transformation
	}

}