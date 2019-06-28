/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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
