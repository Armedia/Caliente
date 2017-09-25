package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface Transformation {

	public <V> void apply(TransformationContext<V> ctx);

}