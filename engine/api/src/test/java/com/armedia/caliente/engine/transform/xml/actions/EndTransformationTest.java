package com.armedia.caliente.engine.transform.xml.actions;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationCompletedException;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Action;

public class EndTransformationTest {

	@Test
	public void test() throws TransformationException {
		Action action = null;
		action = new EndTransformation();

		try {
			action.apply(new TestTransformationContext());
			Assert.fail("Failed to end the transformation");
		} catch (TransformationCompletedException e) {
			// All is well
		}
	}

}