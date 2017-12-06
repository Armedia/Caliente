
package com.armedia.caliente.engine.dynamic.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Action;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;

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
	public final void apply(DynamicElementContext ctx) throws ActionException {
		if (isSkippable()) { return; }
		try {
			if (checkCondition(ctx)) {
				executeAction(ctx);
			}
		} catch (ConditionException e) {
			throw new ActionException(
				String.format("Exception caught checking the condition for a %s", getClass().getSimpleName()), e);
		}
	}

	protected abstract void executeAction(DynamicElementContext ctx) throws ActionException;
}