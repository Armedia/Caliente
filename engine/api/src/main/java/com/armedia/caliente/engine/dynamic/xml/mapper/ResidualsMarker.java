package com.armedia.caliente.engine.dynamic.xml.mapper;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
@FunctionalInterface
public interface ResidualsMarker {

	public boolean isResidualsEnabled();

}