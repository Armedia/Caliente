package com.armedia.caliente.engine.transform;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfValueMapper;

public class TestObjectContext extends DynamicElementContext {

	private final TestObjectFacade object;

	public TestObjectContext() {
		this(new TestAttributeMapper());
	}

	public TestObjectContext(CmfValueMapper mapper) {
		super(null, new TestObjectFacade(), mapper, null);
		this.object = TestObjectFacade.class.cast(super.getDynamicObject());
	}

	@Override
	public TestObjectFacade getDynamicObject() {
		return this.object;
	}

}