package com.armedia.caliente.engine.dynamic;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface Condition {

	public boolean check(DynamicElementContext ctx) throws ConditionException;

}