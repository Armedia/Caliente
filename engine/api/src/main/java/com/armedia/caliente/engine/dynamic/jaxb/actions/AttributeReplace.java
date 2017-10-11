
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceAttribute.t", propOrder = {
	"comparison", "name", "cardinality", "regex", "replacement"
})
public class AttributeReplace extends AbstractReplaceValue {

	@Override
	protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx) {
		return ctx.getTransformableObject().getAtt();
	}

}