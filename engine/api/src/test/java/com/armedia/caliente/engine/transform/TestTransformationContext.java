package com.armedia.caliente.engine.transform;

import com.armedia.caliente.store.CmfValueMapper;

public class TestTransformationContext extends TransformationContext {

	private final TestObjectFacade object;

	public TestTransformationContext() {
		this(new TestAttributeMapper());
	}

	public TestTransformationContext(CmfValueMapper mapper) {
		super(null, new TestObjectFacade(), mapper);
		this.object = TestObjectFacade.class.cast(super.getTransformableObject());
	}

	@Override
	public TestObjectFacade getTransformableObject() {
		return this.object;
	}

}