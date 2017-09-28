
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionalAction.t", propOrder = {
	"condition"
})
public abstract class ConditionalActionT implements Action {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "if", required = false)
	protected ActionConditionT condition;

	public ActionConditionT getCondition() {
		return this.condition;
	}

	public void setCondition(ActionConditionT condition) {
		this.condition = condition;
	}

	@Override
	public final void apply(TransformationContext ctx) {
		final ActionConditionT wrapper = getCondition();
		final Condition condition = (wrapper != null ? wrapper.getCondition() : null);
		// Basically, execute this action if there is no condition given, or if the given condition
		// evaluates to true
		if ((condition == null) || condition.check(ctx)) {
			applyTransformation(ctx);
		}
	}

	protected abstract <V> void applyTransformation(TransformationContext ctx);
}