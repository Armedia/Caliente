
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.transform.xml.Cardinality;
import com.armedia.caliente.engine.transform.xml.CardinalityAdapter;
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionMapAttributeValue.t", propOrder = {
	"comparison", "name", "cardinality", "cases", "defVal"
})
public class MapAttributeValue extends AbstractTransformValue {

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	@XmlElement(name = "case", required = false)
	protected List<MapValueCase> cases;

	@XmlElement(name = "default", required = false)
	protected Expression defVal;

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

	public List<MapValueCase> getCases() {
		if (this.cases == null) {
			this.cases = new ArrayList<>();
		}
		return this.cases;
	}

	public Expression getDefaultValue() {
		return this.defVal;
	}

	public void setDefaultValue(Expression defaultValue) {
		this.defVal = defaultValue;
	}

	protected boolean mapValue(TransformationContext ctx, CmfDataType type, Object candidate,
		AtomicReference<Object> result) throws TransformationException {

		// Apply the comparison to each value in the typed value, and if there's a
		// match, apply the replacement and move on to the next value
		for (MapValueCase c : getCases()) {
			Comparison comparison = c.getComparison();
			Expression comparand = c.getValue();
			if (comparand == null) { throw new TransformationException("No comparand value given to compare against"); }
			Expression replacement = c.getReplacement();
			if (replacement == null) { throw new TransformationException("No value given to replace with"); }

			// All is well, execute!
			if (comparison.check(type, candidate, Expression.eval(comparand, ctx))) {
				result.set(Expression.eval(replacement, ctx));
				return true;
			}
		}

		Expression def = getDefaultValue();
		if (def != null) {
			// If there was no match, apply the default
			result.set(Expression.eval(def, ctx));
			return true;
		}

		// No matches, return false...
		return false;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx, TypedValue candidate) throws TransformationException {
		if (candidate.isEmpty()) { return; }

		final List<Object> newValues = new ArrayList<>(candidate.getSize());
		final Cardinality cardinality = getCardinality();
		final AtomicReference<Object> ref = new AtomicReference<>(null);
		if (cardinality == Cardinality.ALL) {
			for (Object o : candidate.getValues()) {
				if (mapValue(ctx, candidate.getType(), newValues, ref)) {
					newValues.add(ref.get());
				} else {
					newValues.add(o);
				}
			}
		} else {
			for (Object oldValue : candidate.getValues()) {
				newValues.add(oldValue);
			}
			final int targetIndex = (cardinality == Cardinality.FIRST ? 0 : candidate.getSize() - 1);
			final Object oldValue = newValues.remove(targetIndex);
			Object newValue = oldValue;
			if (mapValue(ctx, candidate.getType(), oldValue, ref)) {
				newValue = ref.get();
			}
			newValues.add(targetIndex, newValue);
		}
		candidate.setValues(newValues);
	}

	@Override
	protected Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getObject().getAtt().keySet();
	}

	@Override
	protected TypedValue getCandidate(TransformationContext ctx, String name) {
		return ctx.getObject().getAtt().get(name);
	}

}