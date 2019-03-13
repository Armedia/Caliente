package com.armedia.caliente.cli.utils;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class LibLaunchHelperTest {

	@Test
	public void testLibLaunchHelper() {
		LibLaunchHelper lib = new LibLaunchHelper();
		Assert.assertEquals(LibLaunchHelper.DEFAULT_LIB, lib.getDefault());
		Assert.assertEquals(LibLaunchHelper.LIB_ENV_VAR, lib.getEnvVarName());
	}

	@Test
	public void testLibLaunchHelperString() {
		LibLaunchHelper lib = null;
		String defaultLib = UUID.randomUUID().toString();

		lib = new LibLaunchHelper(null);
		Assert.assertNull(lib.getDefault());
		Assert.assertEquals(LibLaunchHelper.LIB_ENV_VAR, lib.getEnvVarName());

		lib = new LibLaunchHelper(defaultLib);
		Assert.assertEquals(defaultLib, lib.getDefault());
		Assert.assertEquals(LibLaunchHelper.LIB_ENV_VAR, lib.getEnvVarName());
	}

	@Test
	public void testLibLaunchHelperStringString() {
		LibLaunchHelper lib = null;
		String defaultLib = String.format("default-lib-%s", UUID.randomUUID().toString());
		String envVarName = String.format("env-var-%s", UUID.randomUUID().toString());

		lib = new LibLaunchHelper(null, null);
		Assert.assertNull(lib.getDefault());
		Assert.assertNull(lib.getEnvVarName());

		lib = new LibLaunchHelper(defaultLib, null);
		Assert.assertEquals(defaultLib, lib.getDefault());
		Assert.assertNull(lib.getEnvVarName());

		lib = new LibLaunchHelper(null, envVarName);
		Assert.assertNull(lib.getDefault());
		Assert.assertEquals(envVarName, lib.getEnvVarName());

		lib = new LibLaunchHelper(defaultLib, envVarName);
		Assert.assertEquals(defaultLib, lib.getDefault());
		Assert.assertEquals(envVarName, lib.getEnvVarName());
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
