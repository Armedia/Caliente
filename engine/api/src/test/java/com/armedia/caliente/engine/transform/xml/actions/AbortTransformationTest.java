package com.armedia.caliente.engine.transform.xml.actions;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;

public class AbortTransformationTest {

	@Test
	public void test() {
		try {
			new AbortTransformation().apply(new TestTransformationContext());
			Assert.fail("Failed to abort the transformation");
		} catch (TransformationException e) {
			// All is well
		}
	}

}