package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionApplyPrincipalMapping.t", propOrder = {
	"comparison", "name", "type", "cardinality"
})
public class PrincipalMappingApply extends AbstractValueMappingApply<PrincipalType> {

	@Override
	protected String getMappedLabel(DynamicElementContext ctx) throws ActionException {
		return String.format("$%s_NAME$", getType().name());
	}

	@Override
	protected CmfType getMappingType(PrincipalType type) {
		switch (type) {
			case GROUP:
				return CmfType.GROUP;
			case USER:
				return CmfType.USER;
			case ROLE:
			default:
				// TODO: WTF?!?
				break;
		}
		return null;
	}
}