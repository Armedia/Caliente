package com.armedia.caliente.engine.transform;

import com.armedia.caliente.store.CmfValueMapper;

public class TestTransformationContext extends TransformationContext {

	private final TestObjectFacade object;

	public TestTransformationContext() {
		this(new TestAttributeMapper());
	}

	public TestTransformationContext(CmfValueMapper mapper) {
		super(new TestObjectFacade(), mapper);
		this.object = TestObjectFacade.class.cast(super.getObject());
	}

	@Override
	public TestObjectFacade getObject() {
		return this.object;
	}

}