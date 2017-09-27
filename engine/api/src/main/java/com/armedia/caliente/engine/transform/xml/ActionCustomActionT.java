
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.DynamicElementFactory;
import com.armedia.caliente.engine.transform.DynamicElementRegistry;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionCustomAction.t", propOrder = {
	"className"
})
public class ActionCustomActionT extends ConditionalActionT {

	private static final DynamicElementRegistry<Action> ACTIONS = new DynamicElementRegistry<>(Action.class);

	@XmlElement(name = "class-name", required = true)
	protected ExpressionT className;

	public ExpressionT getClassName() {
		return this.className;
	}

	public void setClassName(ExpressionT value) {
		this.className = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		ExpressionT classNameExpr = getClassName();

		String className = Tools.toString(classNameExpr.evaluate(ctx));
		if (className == null) { throw new RuntimeTransformationException(
			String.format("The given %s expression did not return a string value: %s", classNameExpr.getLang(),
				classNameExpr.getValue())); }

		try {
			DynamicElementFactory<Action> factory = ActionCustomActionT.ACTIONS.getFactory(className);
			Action action = factory.acquireInstance();
			try {
				action.apply(ctx);
			} finally {
				factory.releaseInstance(action);
			}
		} catch (Exception e) {
			throw new RuntimeTransformationException(e);
		}
	}
}