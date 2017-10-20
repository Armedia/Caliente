
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Action;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ActionFactory;
import com.armedia.caliente.engine.dynamic.CustomComponents;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionCustomAction.t", propOrder = {
	"className"
})
public class CustomAction extends ConditionalAction {

	@XmlElement(name = "class-name", required = true)
	protected Expression className;

	public Expression getClassName() {
		return this.className;
	}

	public void setClassName(Expression value) {
		this.className = value;
	}

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		String className = Tools.toString(ActionTools.eval(getClassName(), ctx));
		if (className == null) { throw new ActionException("No classname given to insantiate"); }

		final ActionFactory factory = CustomComponents.getActionFactory(className);
		if (factory == null) { throw new ActionException(
			String.format("Failed to locate an action factory for class [%s]", className)); }
		try {
			Action action = factory.acquireInstance(className);
			try {
				action.apply(ctx);
			} finally {
				try {
					factory.releaseInstance(action);
				} catch (Exception e) {
					this.log.warn("Failed to release an Action instance of {}", className, e);
				}
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
}