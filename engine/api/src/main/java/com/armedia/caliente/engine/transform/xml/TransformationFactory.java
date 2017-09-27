package com.armedia.caliente.engine.transform.xml;

import com.armedia.caliente.engine.transform.TransformationContext;

public interface TransformationFactory {

	public <V> Transformation getTransformationInstance(TransformationContext<V> ctx);

}