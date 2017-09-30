
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.ComparisonAdapter;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveDecorator.t", propOrder = {
	"comparison", "decorator"
})
public class DecoratorRemove extends ConditionalAction {

	@XmlElement(name = "comparison", required = true)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	@XmlElement(name = "decorator", required = true)
	protected Expression decorator;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}

	public Expression getDecorator() {
		return this.decorator;
	}

	public void setDecorator(Expression value) {
		this.decorator = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		String decorator = StringUtils.strip(Tools.toString(Expression.eval(getDecorator(), ctx)));
		if (!StringUtils.isEmpty(decorator)) {
			ctx.getDecorators().remove(decorator);
		}
	}

}