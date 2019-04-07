package com.armedia.caliente.cli.utils;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LibLaunchHelperTest {

	@Test
	public void testLibLaunchHelper() {
		LibLaunchHelper lib = new LibLaunchHelper();
		Assertions.assertEquals(LibLaunchHelper.DEFAULT_LIB, lib.getDefault());
		Assertions.assertEquals(LibLaunchHelper.LIB_ENV_VAR, lib.getEnvVarName());
	}

	@Test
	public void testLibLaunchHelperString() {
		LibLaunchHelper lib = null;
		String defaultLib = UUID.randomUUID().toString();

		lib = new LibLaunchHelper(null);
		Assertions.assertNull(lib.getDefault());
		Assertions.assertEquals(LibLaunchHelper.LIB_ENV_VAR, lib.getEnvVarName());

		lib = new LibLaunchHelper(defaultLib);
		Assertions.assertEquals(defaultLib, lib.getDefault());
		Assertions.assertEquals(LibLaunchHelper.LIB_ENV_VAR, lib.getEnvVarName());
	}

	@Test
	public void testLibLaunchHelperStringString() {
		LibLaunchHelper lib = null;
		String defaultLib = String.format("default-lib-%s", UUID.randomUUID().toString());
		String envVarName = String.format("env-var-%s", UUID.randomUUID().toString());

		lib = new LibLaunchHelper(null, null);
		Assertions.assertNull(lib.getDefault());
		Assertions.assertNull(lib.getEnvVarName());

		lib = new LibLaunchHelper(defaultLib, null);
		Assertions.assertEquals(defaultLib, lib.getDefault());
		Assertions.assertNull(lib.getEnvVarName());

		lib = new LibLaunchHelper(null, envVarName);
		Assertions.assertNull(lib.getDefault());
		Assertions.assertEquals(envVarName, lib.getEnvVarName());

		lib = new LibLaunchHelper(defaultLib, envVarName);
		Assertions.assertEquals(defaultLib, lib.getDefault());
		Assertions.assertEquals(envVarName, lib.getEnvVarName());
	}

	@Test
	public void testCollectEntries() {
	}

	@Test
	public void testGetClasspathPatchesPre() {
	}

	@Test
	public void testGetClasspathPatchesPost() {
	}

}
