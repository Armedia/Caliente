package com.armedia.caliente.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AlphaCounterTest {

	@Test
	public void testAlphaCounter() {
		// Test up to 5 digits, case-insensitive
		AlphaCounter.countAlpha("");
		final int base = (('Z' - 'A') + 1);
		final Double limitDouble = Math.pow(base, 5) - 1;
		final int limit = limitDouble.intValue();
		for (int i = 0; i < limit; i++) {
			String str = AlphaCounter.renderAlpha(i);
			Assertions.assertEquals(i, AlphaCounter.countAlpha(str), str);
		}
	}
}