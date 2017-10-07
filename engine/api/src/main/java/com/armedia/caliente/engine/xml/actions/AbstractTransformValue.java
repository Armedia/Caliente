
package com.armedia.caliente.engine.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.xml.Comparison;
import com.armedia.caliente.engine.xml.ComparisonAdapter;
import com.armedia.caliente.engine.xml.ConditionalAction;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.engine.xml.Transformations;
import com.armedia.caliente.store.CmfDataType;
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

	protected abstract Map<String, TypedValue> getCandidateValues(TransformationContext ctx);

	protected abstract void applyTransformation(TransformationContext ctx, TypedValue candidate)
		throws TransformationException;

	protected boolean failShort() {
		return false;
	}

	@Override
	protected final void applyTransformation(TransformationContext ctx) throws TransformationException {
		if (failShort()) { return; }
		final String comparand = Tools.toString(Transformations.eval(getName(), ctx));
		if (comparand == null) { throw new TransformationException("No comparand given to check the name against"); }
		final Comparison comparison = getComparison();

		final Map<String, TypedValue> values = getCandidateValues(ctx);

		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			TypedValue candidate = values.get(comparand);
			if (candidate != null) {
				applyTransformation(ctx, candidate);
			}
			return;
		}

		// Need to find a matching candidate...
		for (String s : values.keySet()) {
			if (comparison.check(CmfDataType.STRING, s, comparand)) {
				applyTransformation(ctx, values.get(s));
			}
		}
	}

}