
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
	"name"
})
public class SecondarySubtypeAdd extends ConditionalAction {

	@XmlElement(name = "name", required = true)
	protected Expression name;

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression name) {
		this.name = name;
	}

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		String secondary = Tools.toString(ActionTools.eval(getName(), ctx));
		if (StringUtils.isEmpty(secondary)) { return; }

		for (String s : Tools.splitCSVEscaped(secondary)) {
			s = StringUtils.strip(s);
			if (!StringUtils.isEmpty(s)) {
				ctx.getDynamicObject().getSecondarySubtypes().add(s);
			}
		}
	}

}