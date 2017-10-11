
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionMapVariableValue.t", propOrder = {
	"comparison", "name", "cardinality", "cases", "defVal"
})
public class MapVariableValue extends AbstractMapValue {

	@Override
	protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx) {
		return ctx.getVariables();
	}

}