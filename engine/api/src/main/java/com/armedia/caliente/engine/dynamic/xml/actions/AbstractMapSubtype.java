
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValueType;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractMapSubtype extends ConditionalAction {

	@XmlElement(name = "case", required = false)
	protected List<MapValueCase> cases;

	@XmlElement(name = "default", required = false)
	protected Expression defVal;

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

	protected final boolean failShort() {
		return (getDefaultValue() == null) && getCases().isEmpty();
	}

	protected final Object mapSubtype(DynamicElementContext ctx, Object candidate) throws ActionException {

		// Apply the comparison to each value in the typed value, and if there's a
		// match, apply the replacement and move on to the next value
		for (MapValueCase c : getCases()) {
			Comparison comparison = c.getComparison();
			Expression comparand = c.getValue();
			if (comparand == null) { throw new ActionException("No comparand value given to compare against"); }
			Expression replacement = c.getReplacement();
			if (replacement == null) { throw new ActionException("No value given to replace with"); }

			// All is well, execute!
			if (comparison.check(CmfValueType.STRING, candidate, ActionTools.eval(comparand, ctx))) {
				// This is the mapping value, so return it!
				return ActionTools.eval(replacement, ctx);
			}
		}

		Expression def = getDefaultValue();
		if (def != null) {
			// If there was no match, apply the default
			return ActionTools.eval(def, ctx);
		}

		// No matches, return null...
		return null;
	}

	protected abstract Object getCandidate(DynamicElementContext ctx);

	@Override
	protected final void executeAction(DynamicElementContext ctx) throws ActionException {
		// Shortcut - avoid any work if no work can be done...
		if (failShort()) { return; }

		Object candidate = getCandidate(ctx);
		Object mappedValue = mapSubtype(ctx, candidate);
		String newSubtype = StringUtils.strip(Tools.toString(mappedValue));
		if (!StringUtils.isEmpty(newSubtype)) {
			ctx.getDynamicObject().setSubtype(newSubtype);
		}
	}

}