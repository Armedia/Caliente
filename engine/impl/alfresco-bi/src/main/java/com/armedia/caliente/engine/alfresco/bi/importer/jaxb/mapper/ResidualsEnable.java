package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "enableResiduals.t")
public class ResidualsEnable extends EmptyElement implements ResidualsMarker {

	@Override
	public boolean isResidualsEnabled() {
		return true;
	}

}