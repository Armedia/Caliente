
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsCalientePropertyEmpty.t")
public class IsCalientePropertyEmpty extends AbstractCalientePropertyCheck {

	@Override
	protected boolean check(DynamicValue candidate) {
		return (candidate == null) || candidate.isEmpty();
	}

}