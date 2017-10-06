package com.armedia.caliente.engine.xml;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;

@XmlTransient
public interface Action {

	public void apply(TransformationContext ctx) throws TransformationException;

}