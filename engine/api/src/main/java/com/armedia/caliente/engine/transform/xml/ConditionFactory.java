package com.armedia.caliente.engine.transform.xml;

import com.armedia.caliente.engine.transform.TransformationContext;

public interface ConditionFactory {

	public <V> Condition getConditionInstance(TransformationContext<V> ctx);

}