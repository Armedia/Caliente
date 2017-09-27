package com.armedia.caliente.engine.transform.xml;

public interface ConditionFactory {

	public <V> Condition getConditionInstance(TransformationContext<V> ctx);

}