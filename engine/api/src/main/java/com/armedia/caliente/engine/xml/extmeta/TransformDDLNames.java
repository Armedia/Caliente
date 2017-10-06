package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataTransformNames.t", propOrder = {
	"map", "transform"
})
public class TransformDDLNames {

	protected List<MetadataNameMapping> map;
	protected Expression transform;

	public List<MetadataNameMapping> getMap() {
		if (this.map == null) {
			this.map = new ArrayList<>();
		}
		return this.map;
	}

	public Expression getTransform() {
		return this.transform;
	}

	public void setTransform(Expression value) {
		this.transform = value;
	}

}