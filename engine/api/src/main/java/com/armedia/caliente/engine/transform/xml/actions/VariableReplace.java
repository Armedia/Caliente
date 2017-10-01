
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceVariable.t", propOrder = {
	"comparison", "name", "cardinality", "regex", "replacement"
})
public class VariableReplace extends AbstractReplaceValue {

	@Override
	protected Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getVariables().keySet();
	}

	@Override
	protected TypedValue getCandidate(TransformationContext ctx, String name) {
		return ctx.getVariables().get(name);
	}

}