package com.armedia.caliente.engine.dynamic;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
@FunctionalInterface
public interface Action {

	public void apply(DynamicElementContext ctx) throws ActionException;

}