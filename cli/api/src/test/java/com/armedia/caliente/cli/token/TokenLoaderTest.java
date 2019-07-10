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
package com.armedia.caliente.cli.token;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenLoaderTest {

	@Test
	public void testParser() {
		List<String> l = Collections.emptyList();
		TokenLoader p = new TokenLoader(new StaticTokenSource("primary", l));
		Assertions.assertNotNull(p);
		Assertions.assertEquals(TokenLoader.DEFAULT_VALUE_SEPARATOR, p.getValueSeparator());
	}

	@Test
	public void testParse() throws Exception {
		String[] args = null;

		args = new String[] {
			"-a", "--bb", "XXX", "-MuLtIsHoRt", "subcommand", "asdfasdf", "--@", "classpath:/test-parameter-file.txt",
			"--", "--@", "res:/test-parameter-file.txt", "--ff"
		};

		for (Token t : new TokenLoader(new StaticTokenSource("primary", Arrays.asList(args)))) {
			System.out.printf("%s%n", t);
		}
	}
}