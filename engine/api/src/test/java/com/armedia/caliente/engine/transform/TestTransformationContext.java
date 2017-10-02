package com.armedia.caliente.engine.transform;

import com.armedia.caliente.store.CmfAttributeMapper;

public class TestTransformationContext extends TransformationContext {

	private final TestObjectFacade object;

	public TestTransformationContext() {
		this(new TestAttributeMapper());
	}

	public TestTransformationContext(CmfAttributeMapper mapper) {
		super(new TestObjectFacade(), mapper);
		this.object = TestObjectFacade.class.cast(getObject());
	}

	@Override
	public TestObjectFacade getObject() {
		return this.object;
	}

}