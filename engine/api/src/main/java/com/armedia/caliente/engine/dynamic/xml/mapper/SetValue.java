package com.armedia.caliente.engine.dynamic.xml.mapper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "setValue.t", propOrder = {
	"value", "type"
})
public class SetValue extends Mapping {

	public static final CmfDataType DEFAULT_TYPE = CmfDataType.STRING;

	@XmlAttribute(name = "type", required = false)
	protected CmfDataType type;

	public final CmfDataType getType() {
		return Tools.coalesce(this.type, SetValue.DEFAULT_TYPE);
	}

	public final void setType(CmfDataType type) {
		this.type = type;
	}

}