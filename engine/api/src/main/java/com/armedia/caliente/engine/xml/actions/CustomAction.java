
package com.armedia.caliente.engine.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ActionException;
import com.armedia.caliente.engine.transform.ActionFactory;
import com.armedia.caliente.engine.transform.DynamicTransformationElements;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.xml.Action;
import com.armedia.caliente.engine.xml.ConditionalAction;
import com.armedia.caliente.engine.xml.Expression;
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
	protected void applyTransformation(ObjectContext ctx) throws ActionException {
		String className = Tools.toString(ActionTools.eval(getClassName(), ctx));
		if (className == null) { throw new ActionException("No classname given to insantiate"); }

		final ActionFactory factory = DynamicTransformationElements.getActionFactory(className);
		if (factory == null) { throw new ActionException(
			String.format("Failed to locate an action factory for class [%s]", className)); }
		try {
			Action action = factory.acquireInstance(className);
			try {
				action.apply(ctx);
			} finally {
				factory.releaseInstance(action);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
}