
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.ConditionalActionT;
import com.armedia.caliente.engine.transform.xml.ExpressionT;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetVariable.t", propOrder = {
	"name", "type", "value"
})
public class SetVariable extends ConditionalActionT {

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfTypeAdapter.class)
	protected CmfType type;

	@XmlElement(name = "value", required = true)
	protected ExpressionT value;

	public ExpressionT getName() {
		return this.name;
	}

	public void setName(ExpressionT value) {
		this.name = value;
	}

	public CmfType getType() {
		return this.type;
	}

	public void setType(CmfType value) {
		this.type = value;
	}

	public ExpressionT getValue() {
		return this.value;
	}

	public void setValue(ExpressionT value) {
		this.value = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation
	}

}