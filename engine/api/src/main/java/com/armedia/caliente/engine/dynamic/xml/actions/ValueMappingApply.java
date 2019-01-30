package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfArchetype;
import com.armedia.caliente.store.xml.CmfTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionApplyValueMapping.t", propOrder = {
	"comparison", "attributeName", "type", "mappingName", "cardinality", "fallback"
})
public class ValueMappingApply extends AbstractValueMappingApply<CmfArchetype> {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfTypeAdapter.class)
	protected CmfArchetype type;

	@XmlElement(name = "mapping-name", required = false)
	protected Expression mappingName;

	public Expression getMappingName() {
		return this.mappingName;
	}

	public void setMappingName(Expression mappingName) {
		this.mappingName = mappingName;
	}

	@Override
	protected String getMappedLabel(DynamicElementContext ctx) throws ActionException {
		return Tools.toString(ActionTools.eval(getMappingName(), ctx));
	}

	@Override
	protected CmfArchetype getMappingType(CmfArchetype type) {
		return type;
	}

	@Override
	public void setType(CmfArchetype type) {
		this.type = type;
	}

	@Override
	public CmfArchetype getType() {
		return this.type;
	}
}