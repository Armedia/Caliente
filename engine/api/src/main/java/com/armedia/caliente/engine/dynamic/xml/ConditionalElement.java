
package com.armedia.caliente.engine.dynamic.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.ImmutableElementContext;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

@XmlTransient
public abstract class ConditionalElement {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "if", required = false)
	protected ConditionWrapper condition;

	public Condition getCondition() {
		if (this.condition == null) { return null; }
		return this.condition.getCondition();
	}

	public ConditionalElement setCondition(Condition condition) {
		if (condition == null) {
			this.condition = null;
		} else {
			this.condition = new ConditionWrapper(condition);
		}
		return this;
	}

	protected final boolean checkCondition(DynamicElementContext ctx) throws ConditionException {
		final Condition condition = getCondition();
		if (condition == null) { return true; }
		ImmutableElementContext immutable = null;
		if (ImmutableElementContext.class.isInstance(ctx)) {
			// Small tweak in hopes of optimization...
			immutable = ImmutableElementContext.class.cast(ctx);
		} else {
			immutable = new ImmutableElementContext(ctx);
		}
		return condition.check(immutable);
	}

	protected final String getObjectDescription(DynamicElementContext ctx) {
		CmfObject<CmfValue> obj = ctx.getBaseObject();
		if (obj == null) { return null; }
		return obj.getDescription();
	}
}