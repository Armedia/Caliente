
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.RuntimeTransformationException;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.ComparisonAdapter;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfProperty;
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

	protected abstract Set<String> getCandidateNames(TransformationContext ctx);

	protected abstract CmfProperty<CmfValue> getCandidate(TransformationContext ctx, String name);

	protected abstract void applyTransformation(TransformationContext ctx, CmfProperty<CmfValue> candidate);

	@Override
	protected final void applyTransformation(TransformationContext ctx) {
		final String comparand = Tools.toString(Expression.eval(getName(), ctx));
		if (comparand == null) { throw new RuntimeTransformationException(
			"No comparand given to check the name against"); }
		final Comparison comparison = getComparison();

		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			CmfProperty<CmfValue> candidate = getCandidate(ctx, comparand);
			if (candidate != null) {
				applyTransformation(ctx, candidate);
			}
			return;
		}

		// Need to find a matching candidate...
		for (String s : getCandidateNames(ctx)) {
			if (comparison.check(CmfDataType.STRING, s, comparand)) {
				applyTransformation(ctx, getCandidate(ctx, s));
			}
		}
	}

}