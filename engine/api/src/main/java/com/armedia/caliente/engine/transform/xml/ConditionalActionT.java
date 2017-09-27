
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionalAction.t", propOrder = {
	"condition"
})
public abstract class ConditionalActionT implements Action {

	@XmlElement(name = "if", required = false)
	protected ConditionT condition;

	public ConditionT getCondition() {
		return this.condition;
	}

	public void setCondition(ConditionT condition) {
		this.condition = condition;
	}

	@Override
	public final <V> void apply(TransformationContext<V> ctx) {
		final ConditionT wrapper = getCondition();
		final Condition condition = (wrapper != null ? wrapper.getCondition() : null);
		if ((wrapper == null) || (condition == null) || condition.check(ctx)) {
			applyTransformation(ctx);
		}
	}

	protected abstract <V> void applyTransformation(TransformationContext<V> ctx);
}