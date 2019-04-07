package com.armedia.caliente.engine.tools;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathToolsTest {

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
			List<String> actual = PathTools.getNumericPaths(n, i);
			Assertions.assertArrayEquals(data[i], actual.toArray());
		}
	}

}