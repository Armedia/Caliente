package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionApplyValueMapping.t", propOrder = {
	"comparison", "name", "type", "mappingName", "cardinality"
})
public class ValueMappingApply extends AbstractValueMappingApply<CmfType> {

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
	protected CmfType getMappingType(CmfType type) {
		return type;
	}
}