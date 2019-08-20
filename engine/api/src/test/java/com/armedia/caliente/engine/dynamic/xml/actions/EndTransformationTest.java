package com.armedia.caliente.engine.dynamic.xml.actions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ProcessingCompletedException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;

public class EndTransformationTest {
	@Test
	public void test() throws ActionException {
		try {
			new EndTransformation().apply(new TestObjectContext());
			Assertions.fail("Failed to end the transformation");
		} catch (ProcessingCompletedException e) {
			// All is well
		}
	}
}