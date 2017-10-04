
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
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

	protected abstract void setNewValue(TransformationContext ctx, String newValue);

	@Override
	protected final void applyTransformation(TransformationContext ctx) throws TransformationException {
		String newValue = Tools.toString(Expression.eval(getValue(), ctx));
		if (newValue == null) { throw new TransformationException("No name given to set"); }
		setNewValue(ctx, newValue);
	}
}