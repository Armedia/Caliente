
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.xml.CmfTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionClearValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class ValueMappingClear extends ConditionalAction {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfTypeAdapter.class)
	protected CmfType type;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "from", required = false)
	protected Expression from;

	@XmlElement(name = "to", required = false)
	protected Expression to;

	public void setType(CmfType type) {
		this.type = type;
	}

	public CmfType getType() {
		return this.type;
	}

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression name) {
		this.name = name;
	}

	public Expression getFrom() {
		return this.from;
	}

	public void setFrom(Expression from) {
		this.from = from;
	}

	public Expression getTo() {
		return this.to;
	}

	public void setTo(Expression to) {
		this.to = to;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		CmfType type = getType();
		if (type == null) { throw new TransformationException(
			"Must provide a type name to associate the mapping with"); }
		String name = Tools.toString(Expression.eval(getName(), ctx));
		if (name == null) { throw new TransformationException("Must provide a mapping name"); }

		String from = Tools.toString(Expression.eval(getFrom(), ctx));
		String to = Tools.toString(Expression.eval(getTo(), ctx));
		if ((from == null) && (to == null)) { throw new TransformationException(
			"Must provide either a sorce or target value to identify the mapping to remove"); }
		if ((from != null) && (to != null)) { throw new TransformationException(
			"Must provide only one of either a sorce or target value to identify the mapping to remove (both provided)"); }

		if (from != null) {
			ctx.getAttributeMapper().clearSourceMapping(type, name, from);
		} else {
			ctx.getAttributeMapper().clearTargetMapping(type, name, to);
		}
	}
}