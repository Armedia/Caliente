
package com.armedia.caliente.engine.dynamic.jaxb.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsCalientePropertyRepeating.t")
public class IsCalientePropertyRepeating extends AbstractCalientePropertyCheck {

	@Override
	protected boolean check(TypedValue candidate) {
		return (candidate != null) && candidate.isRepeating();
	}

}