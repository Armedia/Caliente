
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
@XmlType(name = "actionAddSecondarySubtype.t", propOrder = {
	"value"
})
public class SecondarySubtypeAdd extends ConditionalAction {

	@XmlElement(name = "value", required = true)
	protected Expression value;

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		String secondary = StringUtils.strip(Tools.toString(Expression.eval(getValue(), ctx)));
		if (!StringUtils.isEmpty(secondary)) {
			ctx.getObject().getSecondarySubtypes().add(secondary);
		}
	}

}