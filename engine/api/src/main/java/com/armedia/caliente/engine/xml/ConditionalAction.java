
package com.armedia.caliente.engine.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.transform.ImmutableTransformationContext;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionalAction.t", propOrder = {
	"condition"
})
public abstract class ConditionalAction implements Action {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "if", required = false)
	protected ActionCondition condition;

	public Condition getCondition() {
		if (this.condition == null) { return null; }
		return this.condition.getCondition();
	}

	public ConditionalAction setCondition(Condition condition) {
		if (condition == null) {
			this.condition = null;
		} else {
			this.condition = new ActionCondition(condition);
		}
		return this;
	}

	protected boolean isSkippable() {
		// By default, no action is skippable
		return false;
	}

	@Override
	public final void apply(TransformationContext ctx) throws TransformationException {
		if (isSkippable()) { return; }
		final Condition condition = getCondition();
		// Basically, execute this action if there is no condition given, or if the given condition
		// evaluates to true
		if ((condition == null) || condition.check(new ImmutableTransformationContext(ctx))) {
			applyTransformation(ctx);
		}
	}

	protected final String getObjectDescription(TransformationContext ctx) {
		CmfObject<CmfValue> obj = ctx.getBaseObject();
		if (obj == null) { return null; }
		return String.format("%s (%s)[%s]", obj.getType().name(), obj.getLabel(), obj.getId());
	}

	protected abstract void applyTransformation(TransformationContext ctx) throws TransformationException;
}