package com.armedia.caliente.engine.dynamic.xml.actions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;

public class AbortTransformationTest {
	@Test
	public void test() {
		try {
			new AbortTransformation().apply(new TestObjectContext());
			Assertions.fail("Failed to abort the transformation");
		} catch (ActionException e) {
			// All is well
		}
	}
}