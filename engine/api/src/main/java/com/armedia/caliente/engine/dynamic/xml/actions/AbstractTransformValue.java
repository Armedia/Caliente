
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.ComparisonAdapter;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractTransformValue extends ConditionalAction {

	@XmlElement(name = "comparison", required = true)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression value) {
		this.name = value;
	}

	protected abstract Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx);

	protected abstract void executeAction(DynamicElementContext ctx, DynamicValue candidate) throws ActionException;

	protected boolean failShort() {
		return false;
	}

	@Override
	protected final void executeAction(DynamicElementContext ctx) throws ActionException {
		if (failShort()) { return; }
		final String comparand = Tools.toString(ActionTools.eval(getName(), ctx));
		if (comparand == null) { throw new ActionException("No comparand given to check the name against"); }
		final Comparison comparison = getComparison();

		final Map<String, DynamicValue> values = getCandidateValues(ctx);

		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			DynamicValue candidate = values.get(comparand);
			if (candidate != null) {
				executeAction(ctx, candidate);
			}
			return;
		}

		// Need to find a matching candidate...
		for (String s : values.keySet()) {
			if (comparison.check(CmfValue.Type.STRING, s, comparand)) {
				executeAction(ctx, values.get(s));
			}
		}
	}

}