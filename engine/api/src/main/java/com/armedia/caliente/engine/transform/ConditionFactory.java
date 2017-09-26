package com.armedia.caliente.engine.transform;

public interface ConditionFactory {

	public <V> Condition getConditionInstance(TransformationContext<V> ctx);

}