package com.armedia.caliente.engine.xml;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.ConditionException;
import com.armedia.caliente.engine.transform.ObjectContext;

@XmlTransient
public interface Condition {

	public boolean check(ObjectContext ctx) throws ConditionException;

}