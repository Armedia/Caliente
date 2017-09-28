
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsAttributeEmpty.t")
public class ConditionIsAttributeEmptyT extends ConditionAttributeCheckT {

	@Override
	protected boolean check(CmfAttribute<CmfValue> candidate) {
		return (candidate == null) || !candidate.hasValues();
	}

}