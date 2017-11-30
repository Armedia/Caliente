package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionApplyPrincipalMapping.t", propOrder = {
	"comparison", "name", "type", "cardinality", "fallback"
})
public class PrincipalMappingApply extends AbstractValueMappingApply<PrincipalType> {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(PrincipalTypeAdapter.class)
	protected PrincipalType type;

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

	@Override
	public void setType(PrincipalType type) {
		this.type = type;
	}

	@Override
	public PrincipalType getType() {
		return this.type;
	}
}