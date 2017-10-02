
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionMapAttributeValue.t", propOrder = {
	"comparison", "name", "cardinality", "cases", "defVal"
})
public class MapAttributeValue extends AbstractMapValue {

	@Override
	protected Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getObject().getAtt().keySet();
	}

	@Override
	protected TypedValue getCandidate(TransformationContext ctx, String name) {
		return ctx.getObject().getAtt().get(name);
	}

}