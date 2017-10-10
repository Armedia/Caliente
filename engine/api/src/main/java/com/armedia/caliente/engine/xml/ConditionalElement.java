
package com.armedia.caliente.engine.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.transform.ConditionException;
import com.armedia.caliente.engine.transform.ImmutableObjectContext;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

@XmlTransient
public abstract class ConditionalElement {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "if", required = false)
	protected ActionCondition condition;

	public Condition getCondition() {
		if (this.condition == null) { return null; }
		return this.condition.getCondition();
	}

	public ConditionalElement setCondition(Condition condition) {
		if (condition == null) {
			this.condition = null;
		} else {
			this.condition = new ActionCondition(condition);
		}
		return this;
	}

	protected final boolean checkCondition(ObjectContext ctx) throws ConditionException {
		final Condition condition = getCondition();
		if (condition == null) { return true; }
		return condition.check(new ImmutableObjectContext(ctx));
	}

	protected final String getObjectDescription(ObjectContext ctx) {
		CmfObject<CmfValue> obj = ctx.getBaseObject();
		if (obj == null) { return null; }
		return obj.getDescription();
	}
}