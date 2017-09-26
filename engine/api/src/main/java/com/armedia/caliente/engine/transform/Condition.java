package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface Condition {

	public <V> boolean check(TransformationContext<V> ctx);

}