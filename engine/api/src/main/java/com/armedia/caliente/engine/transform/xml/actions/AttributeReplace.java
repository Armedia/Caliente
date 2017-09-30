
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceAttribute.t", propOrder = {
	"comparison", "name", "cardinality", "regex", "replacement"
})
public class AttributeReplace extends AbstractReplaceValue {

	@Override
	protected Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getAttributeNames();
	}

	@Override
	protected CmfProperty<CmfValue> getCandidate(TransformationContext ctx, String name) {
		return ctx.getAttribute(name);
	}
}