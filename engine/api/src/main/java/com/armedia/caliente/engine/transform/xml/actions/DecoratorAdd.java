
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionAddDecorator.t", propOrder = {
	"decorator"
})
public class DecoratorAdd extends ConditionalAction {

	@XmlElement(name = "decorator", required = true)
	protected Expression decorator;

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
			ctx.getObject().getDecorators().add(decorator);
		}
	}

}