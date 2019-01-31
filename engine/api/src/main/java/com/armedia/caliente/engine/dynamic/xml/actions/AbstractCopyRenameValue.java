
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractCopyRenameValue extends ConditionalAction {

	@XmlElement(name = "from", required = true)
	protected Expression from;

	@XmlElement(name = "to", required = true)
	protected Expression to;

	public Expression getFrom() {
		return this.from;
	}

	public void setFrom(Expression from) {
		this.from = from;
	}

	public Expression getTo() {
		return this.to;
	}

	public void setTo(Expression to) {
		this.to = to;
	}

	protected abstract Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx);

	protected abstract void storeValue(DynamicElementContext ctx, DynamicValue oldValue, DynamicValue newValue);

	@Override
	protected final void executeAction(DynamicElementContext ctx) throws ActionException {
		String from = StringUtils.strip(Tools.toString(ActionTools.eval(getFrom(), ctx)));
		if (StringUtils.isEmpty(from)) { throw new ActionException("No name expression given for element to copy"); }
		String to = StringUtils.strip(Tools.toString(ActionTools.eval(getFrom(), ctx)));
		if (StringUtils.isEmpty(to)) { throw new ActionException("No name expression given for element to create"); }

		final Map<String, DynamicValue> values = getCandidateValues(ctx);
		DynamicValue src = values.get(Tools.toString(from));
		if (src != null) {
			DynamicValue tgt = new DynamicValue(Tools.toString(to), src.getType(), src.isMultivalued());
			tgt.setValues(src.getValues());
			storeValue(ctx, src, tgt);
		}
	}

}