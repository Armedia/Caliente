package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlTransient
public interface Condition {

	public boolean check(TransformationContext ctx);

}