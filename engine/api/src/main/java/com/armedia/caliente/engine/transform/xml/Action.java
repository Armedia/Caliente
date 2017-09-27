package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlTransient
public interface Action {

	public void apply(TransformationContext ctx);

}