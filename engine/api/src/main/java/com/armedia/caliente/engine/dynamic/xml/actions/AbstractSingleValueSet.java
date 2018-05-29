
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractSingleValueSet extends ConditionalAction {

	@XmlElement(name = "name", required = true)
	protected Expression name;

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression name) {
		this.name = name;
	}

	protected abstract void setNewValue(DynamicElementContext ctx, String newValue) throws ActionException;

	@Override
	protected final void executeAction(DynamicElementContext ctx) throws ActionException {
		String newValue = Tools.toString(ActionTools.eval(getName(), ctx));
		if (newValue == null) { throw new ActionException("No value given to set"); }
		setNewValue(ctx, newValue);
	}
}