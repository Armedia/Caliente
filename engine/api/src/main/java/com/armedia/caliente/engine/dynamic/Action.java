package com.armedia.caliente.engine.dynamic;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface Action {

	public void apply(DynamicElementContext ctx) throws ActionException;

}