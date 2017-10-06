
package com.armedia.caliente.engine.xml.actions;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.xml.Cardinality;
import com.armedia.caliente.engine.xml.CardinalityAdapter;
import com.armedia.caliente.engine.xml.Comparison;
import com.armedia.caliente.engine.xml.ComparisonAdapter;
import com.armedia.caliente.engine.xml.ConditionalAction;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.store.CmfValueMapper.Mapping;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.xml.CmfTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionApplyValueMapping.t", propOrder = {
	"comparison", "name", "type", "mappingName", "cardinality"
})
public class ValueMappingApply extends ConditionalAction {

	@XmlElement(name = "comparison", required = false)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfTypeAdapter.class)
	protected CmfType type;

	@XmlElement(name = "mapping-name", required = false)
	protected Expression mappingName;

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.EQI);
	}

	public void setComparison(Comparison comparison) {
		this.comparison = comparison;
	}

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression name) {
		this.name = name;
	}

	public void setType(CmfType type) {
		this.type = type;
	}

	public CmfType getType() {
		return this.type;
	}

	public Expression getMappingName() {
		return this.mappingName;
	}

	public void setMappingName(Expression mappingName) {
		this.mappingName = mappingName;
	}

	public Cardinality getCardinality() {
		return this.cardinality;
	}

	public void setCardinality(Cardinality cardinality) {
		this.cardinality = cardinality;
	}

	private String mapValue(TransformationContext ctx, CmfType mappingType, String mappingName, String sourceValue,
		CmfDataType targetType) throws TransformationException {
		Mapping m = ctx.getAttributeMapper().getTargetMapping(mappingType, mappingName, sourceValue);
		return (m != null ? m.getTargetValue() : null);
	}

	private void applyMapping(TransformationContext ctx, CmfType type, String mappingName, TypedValue candidate)
		throws TransformationException {

		if (!candidate.isRepeating()) {
			// Cardinality is irrelevant...
			String oldString = Tools.toString(candidate.getValue());
			String newString = mapValue(ctx, type, mappingName, oldString, candidate.getType());
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
						String newValue = mapValue(ctx, type, mappingName, oldString, candidate.getType());
						newValues.add(Tools.coalesce(newValue, oldValue));
					}
					break;

				case FIRST:
				case LAST:
					for (Object oldValue : candidate.getValues()) {
						newValues.add(oldValue);
					}
					int targetIndex = (cardinality == Cardinality.FIRST ? 0 : valueCount - 1);
					String oldString = Tools.toString(newValues.remove(targetIndex));
					String newValue = mapValue(ctx, type, mappingName, oldString, candidate.getType());
					newValues.add(targetIndex, Tools.coalesce(newValue, oldString));
					break;
			}
			candidate.setValues(newValues);
		}
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		final CmfType type = getType();
		if (type == null) { throw new TransformationException("No type name given to find the mapping"); }
		final String comparand = Tools.toString(Expression.eval(getName(), ctx));
		if (comparand == null) { throw new TransformationException("No comparand given to check the name against"); }
		final Comparison comparison = getComparison();
		final String mappingName = Tools.toString(Expression.eval(getMappingName(), ctx));
		if (mappingName == null) { throw new TransformationException("No mapping name given to apply"); }

		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			TypedValue candidate = ctx.getObject().getAtt().get(comparand);
			if (candidate != null) {
				applyMapping(ctx, type, mappingName, candidate);
			}
			return;
		}

		// Need to find a matching candidate...
		for (String s : ctx.getObject().getAtt().keySet()) {
			if (comparison.check(CmfDataType.STRING, s, comparand)) {
				applyMapping(ctx, type, mappingName, ctx.getObject().getAtt().get(s));
			}
		}
	}
}