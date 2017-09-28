
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Condition;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsFirstVersion.t")
public class ConditionIsFirstVersionT implements Condition {

	@Override
	public boolean check(TransformationContext ctx) {
		CmfProperty<CmfValue> index = ctx.getProperty(IntermediateProperty.VERSION_INDEX);
		CmfValue v = ((index != null) && index.hasValues() ? index.getValue() : null);
		return ((v == null) || v.isNull() || (v.asInteger() == 1));
	}

}