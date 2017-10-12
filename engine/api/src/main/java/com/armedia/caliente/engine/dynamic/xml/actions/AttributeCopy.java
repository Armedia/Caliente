package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionCopyAttribute.t", propOrder = {
	"from", "to"
})
public class AttributeCopy extends AbstractCopyRenameValue {

	@Override
	protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx) {
		return ctx.getDynamicObject().getAtt();
	}

	@Override
	protected void storeValue(DynamicElementContext ctx, DynamicValue src, DynamicValue copy) {
		ctx.getDynamicObject().getAtt().put(copy.getName(), copy);
	}

}