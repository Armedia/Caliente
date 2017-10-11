
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
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
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		String secondary = StringUtils.strip(Tools.toString(ActionTools.eval(getValue(), ctx)));
		if (!StringUtils.isEmpty(secondary)) {
			ctx.getTransformableObject().getSecondarySubtypes().add(secondary);
		}
	}

}