
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionMapVariableValue.t", propOrder = {
	"comparison", "name", "cardinality", "cases", "defVal"
})
public class MapVariableValue extends AbstractMapValue {

	@Override
	protected final Map<String, TypedValue> getCandidateValues(TransformationContext ctx) {
		return ctx.getVariables();
	}

}