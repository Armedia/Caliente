package com.armedia.caliente.engine.transform.xml;

public interface TransformationFactory {

	public <V> Transformation getTransformationInstance(TransformationContext<V> ctx);

}