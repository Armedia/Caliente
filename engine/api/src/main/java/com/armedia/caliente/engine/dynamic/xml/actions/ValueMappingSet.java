
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
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.xml.CmfTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class ValueMappingSet extends ConditionalAction {

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
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		CmfType type = getType();
		if (type == null) { throw new ActionException("Must provide a type name to associate the mapping with"); }
		String name = Tools.toString(ActionTools.eval(getName(), ctx));
		if (name == null) { throw new ActionException("Must provide a mapping name"); }
		String from = Tools.toString(ActionTools.eval(getFrom(), ctx));
		if (from == null) { throw new ActionException("Must provide a source value to map from"); }
		String to = Tools.toString(ActionTools.eval(getTo(), ctx));
		if (to == null) { throw new ActionException("Must provide a target value map into"); }

		ctx.getAttributeMapper().setMapping(type, name, from, to);
	}
}