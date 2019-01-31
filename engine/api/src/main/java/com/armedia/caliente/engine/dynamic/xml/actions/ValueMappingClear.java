
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.xml.CmfObjectArchetypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionClearValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class ValueMappingClear extends ConditionalAction {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfObjectArchetypeAdapter.class)
	protected CmfObject.Archetype type;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "from", required = false)
	protected Expression from;

	@XmlElement(name = "to", required = false)
	protected Expression to;

	public void setType(CmfObject.Archetype type) {
		this.type = type;
	}

	public CmfObject.Archetype getType() {
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
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		final CmfObject.Archetype type = Tools.coalesce(getType(), ctx.getDynamicObject().getType());
		String name = Tools.toString(ActionTools.eval(getName(), ctx));
		if (name == null) { throw new ActionException("Must provide a mapping name"); }

		String from = Tools.toString(ActionTools.eval(getFrom(), ctx));
		String to = Tools.toString(ActionTools.eval(getTo(), ctx));
		if ((from == null) && (to == null)) {
			throw new ActionException("Must provide either a sorce or target value to identify the mapping to remove");
		}
		if ((from != null) && (to != null)) {
			throw new ActionException(
				"Must provide only one of either a sorce or target value to identify the mapping to remove (both provided)");
		}

		if (from != null) {
			ctx.getAttributeMapper().clearSourceMapping(type, name, from);
		} else {
			ctx.getAttributeMapper().clearTargetMapping(type, name, to);
		}
	}
}