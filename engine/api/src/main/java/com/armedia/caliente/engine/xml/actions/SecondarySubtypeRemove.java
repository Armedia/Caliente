
package com.armedia.caliente.engine.xml.actions;

import java.util.Iterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.xml.Comparison;
import com.armedia.caliente.engine.xml.ComparisonAdapter;
import com.armedia.caliente.engine.xml.ConditionalAction;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveSecondarySubtype.t", propOrder = {
	"comparison", "value"
})
public class SecondarySubtypeRemove extends ConditionalAction {

	@XmlElement(name = "comparison", required = true)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	@XmlElement(name = "value", required = true)
	protected Expression value;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		String comparand = StringUtils.strip(Tools.toString(Expression.eval(getValue(), ctx)));
		if (StringUtils.isEmpty(comparand)) { return; }

		Comparison comparison = getComparison();
		if (comparison == Comparison.EQ) {
			// Shortcut
			ctx.getObject().getSecondarySubtypes().remove(comparand);
			return;
		}

		Iterator<String> it = ctx.getObject().getSecondarySubtypes().iterator();
		while (it.hasNext()) {
			String current = it.next();
			if (comparison.check(CmfDataType.STRING, current, comparand)) {
				it.remove();
			}
		}
	}

}