
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.ConditionalActionT;
import com.armedia.caliente.engine.transform.xml.ExpressionT;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetSubtype.t", propOrder = {
	"subtype"
})
public class SetSubtype extends ConditionalActionT {

	@XmlElement(name = "subtype", required = true)
	protected ExpressionT subtype;

	public ExpressionT getSubtype() {
		return this.subtype;
	}

	public void setSubtype(ExpressionT value) {
		this.subtype = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation
	}

}