package com.armedia.caliente.engine.transform;

import com.armedia.caliente.store.CmfAttributeMapper;

public class TestTransformationContext extends TransformationContext {

	public TestTransformationContext() {
		this(new TestAttributeMapper());
	}

	public TestTransformationContext(CmfAttributeMapper mapper) {
		super(new TestObjectData(), mapper);
	}

}