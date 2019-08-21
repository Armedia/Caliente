package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;

public class DebugTest {
	@Test
	public void test() throws ActionException {
		final AtomicInteger counter = new AtomicInteger(0);
		Debug debug = new Debug();
		debug.getActions().add((ctx) -> counter.incrementAndGet());

		Assertions.assertEquals(0, counter.get());
		debug.apply(new TestObjectContext());
		Assertions.assertEquals(1, counter.get());
	}
}