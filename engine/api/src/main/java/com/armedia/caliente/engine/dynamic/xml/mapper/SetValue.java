package com.armedia.caliente.engine.dynamic.xml.mapper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "setValue.t", propOrder = {
	"value", "type"
})
public class SetValue extends Mapping {

	public static final CmfValue.Type DEFAULT_TYPE = CmfValue.Type.STRING;

	@XmlAttribute(name = "type", required = false)
	protected CmfValue.Type type;

	public final CmfValue.Type getType() {
		return Tools.coalesce(this.type, SetValue.DEFAULT_TYPE);
	}

	public final void setType(CmfValue.Type type) {
		this.type = type;
	}

}