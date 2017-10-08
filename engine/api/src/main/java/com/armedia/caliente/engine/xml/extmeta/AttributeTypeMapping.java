package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.xml.ExpressionException;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.xml.CmfDataTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataAttributeTypes.t", propOrder = {
	"attributes", "defaultType"
})
public class AttributeTypeMapping {

	@XmlElement(name = "attribute", required = false)
	protected List<MetadataTypeMapping> attributes;

	@XmlElement(name = "default", required = false)
	@XmlJavaTypeAdapter(CmfDataTypeAdapter.class)
	protected CmfDataType defaultType;

	public List<MetadataTypeMapping> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new ArrayList<>();
		}
		return this.attributes;
	}

	public CmfDataType getDefaultType() {
		return this.defaultType;
	}

	public void setDefaultType(CmfDataType value) {
		this.defaultType = value;
	}

	public CmfDataType getAttributeType(final String sqlName) throws ExpressionException {
		for (MetadataTypeMapping mapping : getAttributes()) {
			if (Tools.equals(mapping.name, sqlName)) { return Tools.coalesce(mapping.type, this.defaultType); }
		}
		return this.defaultType;
	}
}