package com.armedia.caliente.engine.dynamic;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
@FunctionalInterface
public interface Condition {

	public boolean check(DynamicElementContext ctx) throws ConditionException;

}