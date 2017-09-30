
package com.armedia.caliente.engine.transform.xml.actions;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Cardinality;
import com.armedia.caliente.engine.transform.xml.CardinalityAdapter;
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.ComparisonAdapter;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfAttributeMapper.Mapping;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
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

	private CmfValue mapValue(TransformationContext ctx, CmfType mappingType, String mappingName, String sourceValue,
		CmfDataType targetType) throws TransformationException {
		Mapping m = ctx.getTargetMapping(mappingType, mappingName, sourceValue);
		if (m == null) { return null; }
		String newValue = m.getTargetValue();
		if (newValue == null) { return CmfValue.NULL.get(targetType); }
		try {
			// TODO: Is this the proper way to do the conversion? Should we even try?
			return new CmfValue(targetType, newValue);
		} catch (ParseException e) {
			throw new TransformationException(
				String.format("Failed to convert the value [%s] as a %s", newValue, targetType.name()), e);
		}
	}

	private void applyMapping(TransformationContext ctx, CmfType type, String mappingName,
		CmfProperty<CmfValue> candidate) throws TransformationException {

		if (!candidate.isRepeating()) {
			// Cardinality is irrelevant...
			CmfValue oldValue = candidate.getValue();
			String oldString = (oldValue != null ? oldValue.asString() : null);
			CmfValue newValue = mapValue(ctx, type, mappingName, oldString, candidate.getType());
			if (newValue != null) {
				candidate.setValue(newValue);
			}
			return;
		}

		final int valueCount = candidate.getValueCount();
		if (valueCount > 0) {
			final List<CmfValue> newValues = new LinkedList<>();
			final Cardinality cardinality = getCardinality();
			switch (cardinality) {
				case ALL:
					for (CmfValue oldValue : candidate) {
						String oldString = (oldValue != null ? oldValue.asString() : null);
						CmfValue newValue = mapValue(ctx, type, mappingName, oldString, candidate.getType());
						newValues.add(Tools.coalesce(newValue, oldValue));
					}
					break;

				case FIRST:
				case LAST:
					for (CmfValue oldValue : candidate) {
						newValues.add(oldValue);
					}
					int targetIndex = (cardinality == Cardinality.FIRST ? 0 : valueCount - 1);
					CmfValue oldValue = newValues.remove(targetIndex);
					String oldString = (oldValue != null ? oldValue.asString() : null);
					CmfValue newValue = mapValue(ctx, type, mappingName, oldString, candidate.getType());
					newValues.add(targetIndex, Tools.coalesce(newValue, oldValue));
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
			CmfProperty<CmfValue> candidate = ctx.getAttribute(comparand);
			if (candidate != null) {
				applyMapping(ctx, type, mappingName, candidate);
			}
			return;
		}

		// Need to find a matching candidate...
		for (String s : ctx.getAttributeNames()) {
			if (comparison.check(CmfDataType.STRING, s, comparand)) {
				applyMapping(ctx, type, mappingName, ctx.getAttribute(s));
			}
		}
	}
}