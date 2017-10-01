
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectDataMember;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsAttributeEmpty.t")
public class IsAttributeEmpty extends AbstractAttributeCheck {

	@Override
	protected boolean check(ObjectDataMember candidate) {
		return (candidate == null) || candidate.isEmpty();
	}

}