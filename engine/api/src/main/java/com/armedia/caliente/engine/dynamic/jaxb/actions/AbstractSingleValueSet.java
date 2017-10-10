
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.jaxb.ConditionalAction;
import com.armedia.caliente.engine.dynamic.jaxb.Expression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractSingleValueSet extends ConditionalAction {

	@XmlElement(name = "value", required = true)
	protected Expression value;

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	protected abstract void setNewValue(ObjectContext ctx, String newValue) throws ActionException;

	@Override
	protected final void applyTransformation(ObjectContext ctx) throws ActionException {
		String newValue = Tools.toString(ActionTools.eval(getValue(), ctx));
		if (newValue == null) { throw new ActionException("No value given to set"); }
		setNewValue(ctx, newValue);
	}
}