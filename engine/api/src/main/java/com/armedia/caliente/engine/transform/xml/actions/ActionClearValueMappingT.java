
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
@XmlType(name = "actionClearValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class ActionClearValueMappingT extends ConditionalActionT {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfTypeAdapter.class)
	protected CmfType type;

	@XmlElement(name = "name", required = true)
	protected ExpressionT name;

	@XmlElement(name = "from", required = false)
	protected ExpressionT from;

	@XmlElement(name = "to", required = false)
	protected ExpressionT to;

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

	public ExpressionT getFrom() {
		return this.from;
	}

	public void setFrom(ExpressionT from) {
		this.from = from;
	}

	public ExpressionT getTo() {
		return this.to;
	}

	public void setTo(ExpressionT to) {
		this.to = to;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext ctx) {
		// TODO Implement this transformation
	}
}