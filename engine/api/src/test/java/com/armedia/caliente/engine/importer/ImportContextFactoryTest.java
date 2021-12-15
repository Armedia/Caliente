package com.armedia.caliente.engine.importer;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ImportContextFactoryTest {

	@Test
	public void testGetTargetPath() throws ImportException {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> ImportContextFactory.getTargetPath(null, 0, null));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> ImportContextFactory.getTargetPath("abcde", 0, null));

		Assertions.assertThrows(ImportException.class,
			() -> ImportContextFactory.getTargetPath("/a/b/c", 4, Collections.emptyList()));

		// We first remove the paths from the original path we're looking for,
		// before grafting it into our root path structure

		Assertions.assertEquals("", ImportContextFactory.getTargetPath("/a/b/c/d/e", 5, Collections.emptyList()));
		Assertions.assertEquals("/e", ImportContextFactory.getTargetPath("/a/b/c/d/e", 4, Collections.emptyList()));
		Assertions.assertEquals("/d/e", ImportContextFactory.getTargetPath("/a/b/c/d/e", 3, Collections.emptyList()));
		Assertions.assertEquals("/c/d/e", ImportContextFactory.getTargetPath("/a/b/c/d/e", 2, Collections.emptyList()));
		Assertions.assertEquals("/b/c/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 1, Collections.emptyList()));
		Assertions.assertEquals("/a/b/c/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 0, Collections.emptyList()));

		Assertions.assertEquals("/crap", ImportContextFactory.getTargetPath("/a/b/c/d/e", 5, Arrays.asList("crap")));
		Assertions.assertEquals("/crap/e", ImportContextFactory.getTargetPath("/a/b/c/d/e", 4, Arrays.asList("crap")));
		Assertions.assertEquals("/crap/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 3, Arrays.asList("crap")));
		Assertions.assertEquals("/crap/c/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 2, Arrays.asList("crap")));
		Assertions.assertEquals("/crap/b/c/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 1, Arrays.asList("crap")));
		Assertions.assertEquals("/crap/a/b/c/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 0, Arrays.asList("crap")));

		Assertions.assertEquals("/crap/ola",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 5, Arrays.asList("crap", "ola")));
		Assertions.assertEquals("/crap/ola/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 4, Arrays.asList("crap", "ola")));
		Assertions.assertEquals("/crap/ola/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 3, Arrays.asList("crap", "ola")));
		Assertions.assertEquals("/crap/ola/c/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 2, Arrays.asList("crap", "ola")));
		Assertions.assertEquals("/crap/ola/b/c/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 1, Arrays.asList("crap", "ola")));
		Assertions.assertEquals("/crap/ola/a/b/c/d/e",
			ImportContextFactory.getTargetPath("/a/b/c/d/e", 0, Arrays.asList("crap", "ola")));

	}
}
