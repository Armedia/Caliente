/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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

import java.io.Console;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.commons.utilities.Tools;

public class CliValuePrompt {

	public static class ConsolePrompter implements Supplier<char[]> {
		private final boolean echoInput;
		private final String prompt;
		private final Object[] promptParams;

		public ConsolePrompter(boolean echoInput, String prompt, Object... promptParams) {
			this.echoInput = echoInput;
			this.prompt = Tools.coalesce(prompt, "Password:");
			if (promptParams != null) {
				this.promptParams = promptParams.clone();
			} else {
				this.promptParams = ArrayUtils.EMPTY_OBJECT_ARRAY;
			}
		}

		@Override
		public char[] get() {
			final Console console = System.console();
			if (console == null) { return null; }

			if (this.echoInput) { return CliValuePrompt.getChars(console.readLine(this.prompt, this.promptParams)); }
			char[] pass = console.readPassword(this.prompt, this.promptParams);
			if (pass != null) { return pass; }
			return null;
		}
	}

	public static char[] getChars(String v) {
		if (v == null) { return null; }
		if (v.length() == 0) { return ArrayUtils.EMPTY_CHAR_ARRAY; }
		return v.toCharArray();
	}

	public static String getString(char[] c) {
		if (c == null) { return null; }
		if (c.length == 0) { return StringUtils.EMPTY; }
		return new String(c);
	}

	public static final String getUsername(OptionValues cli, Supplier<Option> param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getUsername(cli, Option.unwrap(param), prompt, promptParams);
	}

	public static final String getUsername(OptionValues cli, Option param, String prompt, Object... promptParams) {
		return CliValuePrompt
			.getString(CliValuePrompt.getPromptableValue(cli, param, new ConsolePrompter(true, prompt, promptParams)));
	}

	public static final String getPasswordString(OptionValues cli, Supplier<Option> param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getPasswordString(cli, Option.unwrap(param), prompt, promptParams);
	}

	public static final char[] getPassword(OptionValues cli, Supplier<Option> param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getPassword(cli, Option.unwrap(param), prompt, promptParams);
	}

	public static final String getPasswordString(OptionValues cli, Option param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getString(CliValuePrompt.getPassword(cli, param, prompt, promptParams));
	}

	public static final char[] getPassword(OptionValues cli, Option param, String prompt, Object... promptParams) {
		return CliValuePrompt.getPromptableValue(cli, param, new ConsolePrompter(false, prompt, promptParams));
	}

	public static final char[] getPromptableValue(OptionValues cli, Supplier<Option> param,
		Supplier<char[]> promptCallback) {
		return CliValuePrompt.getPromptableValue(cli, Option.unwrap(param), promptCallback);
	}

	public static final char[] getPromptableValue(OptionValues cli, Option param, Supplier<char[]> promptCallback) {
		// If the option is given, return its value
		if ((cli != null) && (param != null) && cli.isPresent(param)) {
			return CliValuePrompt.getChars(cli.getString(param));
		}
		if (promptCallback == null) { return null; }
		return promptCallback.get();
	}
}