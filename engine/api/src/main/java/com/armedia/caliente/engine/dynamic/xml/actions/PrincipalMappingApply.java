package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.PrincipalType;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Cardinality;
import com.armedia.caliente.engine.dynamic.xml.CardinalityAdapter;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.ComparisonAdapter;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionApplyPrincipalMapping.t", propOrder = {
	"comparison", "names", "type", "cardinality", "fallback"
})
public class PrincipalMappingApply extends ConditionalAction {

	@XmlElement(name = "comparison", required = false)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	@XmlElement(name = "name", required = true)
	protected List<Expression> names;

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(PrincipalTypeAdapter.class)
	protected PrincipalType type;

	@XmlElement(name = "fallback", required = false)
	protected Expression fallback;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public void setComparison(Comparison comparison) {
		this.comparison = comparison;
	}

	public List<Expression> getNames() {
		if (this.names == null) {
			this.names = new ArrayList<>();
		}
		return this.names;
	}

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality cardinality) {
		this.cardinality = cardinality;
	}

	public PrincipalType getType() {
		return this.type;
	}

	public void setType(PrincipalType type) {
		this.type = type;
	}

	public Expression getFallback() {
		return this.fallback;
	}

	public void setFallback(Expression fallback) {
		this.fallback = fallback;
	}

	private String performMapping(DynamicElementContext ctx, String oldString) throws ActionException {
		String newString = getType().mapName(ctx.getAttributeMapper(), oldString);
		if (newString == null) {
			// Try a fallback value
			newString = Tools.toString(ActionTools.eval(getFallback(), ctx));
		}
		if ((newString == null) || StringUtils.equals(oldString, newString)) { return null; }
		return newString;
	}

	private void applyMapping(DynamicElementContext ctx, DynamicValue candidate) throws ActionException {

		if (!candidate.isRepeating()) {
			// Cardinality is irrelevant...
			String oldString = Tools.toString(candidate.getValue());
			String newString = performMapping(ctx, oldString);
			if (newString != null) {
				candidate.setValue(newString);
			}
			return;
		}

		final int valueCount = candidate.getSize();
		if (valueCount > 0) {
			final List<Object> newValues = new LinkedList<>();
			final Cardinality cardinality = getCardinality();
			switch (cardinality) {
				case ALL:
					for (Object oldValue : candidate.getValues()) {
						String oldString = Tools.toString(oldValue);
						String newString = performMapping(ctx, oldString);
						newValues.add(Tools.coalesce(newString, oldString));
					}
					break;

				case FIRST:
				case LAST:
					for (Object oldValue : candidate.getValues()) {
						newValues.add(oldValue);
					}
					int targetIndex = (cardinality == Cardinality.FIRST ? 0 : valueCount - 1);
					String oldString = Tools.toString(newValues.remove(targetIndex));
					String newString = performMapping(ctx, oldString);
					newValues.add(targetIndex, Tools.coalesce(newString, oldString));
					break;
			}
			candidate.setValues(newValues);
		}
	}

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		final Comparison comparison = getComparison();

		for (Expression name : getNames()) {
			final String comparand = Tools.toString(ActionTools.eval(name, ctx));
			if (comparand == null) {
				continue;
			}
			if (comparison == Comparison.EQ) {
				// Shortcut!! Look for only one candidate!
				DynamicValue candidate = ctx.getDynamicObject().getAtt().get(comparand);
				if (candidate != null) {
					applyMapping(ctx, candidate);
				}
				continue;
			}

			// Need to find a matching candidate...
			for (String s : ctx.getDynamicObject().getAtt().keySet()) {
				if (comparison.check(CmfValue.Type.STRING, s, comparand)) {
					applyMapping(ctx, ctx.getDynamicObject().getAtt().get(s));
				}
			}
		}
	}
}