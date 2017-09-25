package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface Condition {

	public boolean evaluate(TransformationContext ctx);
}