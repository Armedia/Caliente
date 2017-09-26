package com.armedia.caliente.engine.transform;

public interface TransformationFactory {

	public <V> Transformation getTransformationInstance(TransformationContext<V> ctx);

}