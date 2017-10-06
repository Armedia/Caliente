
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasAttribute.t")
public class HasAttribute extends AbstractAttributeCheck {

	@Override
	protected boolean check(TypedValue candidate) {
		return (candidate != null);
	}

}