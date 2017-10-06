
package com.armedia.caliente.engine.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceAttribute.t", propOrder = {
	"comparison", "name", "cardinality", "regex", "replacement"
})
public class AttributeReplace extends AbstractReplaceValue {

	@Override
	protected final Map<String, TypedValue> getCandidateValues(TransformationContext ctx) {
		return ctx.getObject().getAtt();
	}

}