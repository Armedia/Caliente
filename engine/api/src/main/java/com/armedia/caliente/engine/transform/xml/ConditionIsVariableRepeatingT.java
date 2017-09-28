
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsVariableRepeating.t")
public class ConditionIsVariableRepeatingT extends ConditionVariableCheckT {

	@Override
	protected boolean check(CmfProperty<CmfValue> candidate) {
		return (candidate != null) && candidate.isRepeating();
	}

}