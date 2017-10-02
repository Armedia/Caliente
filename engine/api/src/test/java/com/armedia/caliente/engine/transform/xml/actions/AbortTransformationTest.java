package com.armedia.caliente.engine.transform.xml.actions;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Action;

public class AbortTransformationTest {

	@Test
	public void test() {
		Action action = null;
		action = new AbortTransformation();

		try {
			action.apply(new TestTransformationContext());
			Assert.fail("Failed to abort the transformation");
		} catch (TransformationException e) {
			// All is well
		}
	}

}