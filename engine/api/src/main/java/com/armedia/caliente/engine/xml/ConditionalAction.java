
package com.armedia.caliente.engine.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ActionException;
import com.armedia.caliente.engine.transform.ConditionException;
import com.armedia.caliente.engine.transform.ObjectContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionalAction.t", propOrder = {
	"condition"
})
public abstract class ConditionalAction extends ConditionalElement implements Action {

	protected boolean isSkippable() {
		// By default, no action is skippable
		return false;
	}

	@Override
	public final void apply(ObjectContext ctx) throws ActionException {
		if (isSkippable()) { return; }
		try {
			if (!checkCondition(ctx)) { return; }
		} catch (ConditionException e) {
			throw new ActionException(
				String.format("Exception caught checking the condition for a %s", getClass().getSimpleName()), e);
		}
		applyTransformation(ctx);
	}

	protected abstract void applyTransformation(ObjectContext ctx) throws ActionException;
}