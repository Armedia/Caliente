package com.armedia.caliente.engine.alfresco.bi;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class AlfCommonTest {

	@Test
	public void testGetNumericPathsLong() {
		long n = 0x102a3b4c5d6e7f89L;
		String[][] data = {
			null, {
				"102a3b4c5d6e7f", "102a3b4c5d6e7f89"
			}, {
				"102a3b4c5d6e", "7f", "102a3b4c5d6e7f89"
			}, {
				"102a3b4c5d", "6e", "7f", "102a3b4c5d6e7f89"
			}, {
				"102a3b4c", "5d", "6e", "7f", "102a3b4c5d6e7f89"
			}, {
				"102a3b", "4c", "5d", "6e", "7f", "102a3b4c5d6e7f89"
			}, {
				"102a", "3b", "4c", "5d", "6e", "7f", "102a3b4c5d6e7f89"
			}, {
				"10", "2a", "3b", "4c", "5d", "6e", "7f", "102a3b4c5d6e7f89"
			},
		};
		for (int i = 1; i < 8; i++) {
			List<String> expected = Arrays.asList(data[i]);
			List<String> actual = AlfCommon.getNumericPaths(n, i);
			Assert.assertEquals(expected, actual);
		}
	}

}