
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "switchValue.t", propOrder = {
	"_case", "_default"
})
public class SwitchValueT {

	@XmlElement(name = "case", required = true)
	protected List<SwitchCaseT> _case;

	@XmlElement(name = "default")
	protected ExpressionT _default;

	public List<SwitchCaseT> getCase() {
		if (this._case == null) {
			this._case = new ArrayList<>();
		}
		return this._case;
	}

	public ExpressionT getDefault() {
		return this._default;
	}

	public void setDefault(ExpressionT value) {
		this._default = value;
	}

	public <V> String selectValue(TransformationContext<V> ctx, String candidate) {
		for (SwitchCaseT c : getCase()) {
			if (c == null) {
				continue;
			}

			SwitchCaseMatchT match = c.getMatch();
			if (match == null) {
				continue;
			}

			Comparison comparison = match.getComparison();
			String comparand = match.evaluate(ctx);

			if (comparison.eval(comparand, candidate)) {
				ExpressionT e = c.getValue();
				return (e != null ? e.evaluate(ctx) : candidate);
			}
		}

		// If there was no match inside, then we use the default, if available
		return (this._default != null ? this._default.evaluate(ctx) : candidate);
	}
}