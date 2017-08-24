package com.armedia.caliente.cli.utils;

import java.io.Console;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.CommandLineValues;
import com.armedia.caliente.cli.ParameterDefinition;
import com.armedia.caliente.cli.ParameterTools;
import com.armedia.caliente.cli.ParameterWrapper;
import com.armedia.commons.utilities.Tools;

public class CliValuePrompt {

	public interface PromptCallback {
		public char[] promptForValue();
	}

	public static class ConsolePrompter implements PromptCallback {
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
		public char[] promptForValue() {
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

	public static final String getUsername(CommandLineValues cli, ParameterWrapper param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getUsername(cli, ParameterTools.unwrap(param), prompt, promptParams);
	}

	public static final String getUsername(CommandLineValues cli, ParameterDefinition param, String prompt,
		Object... promptParams) {
		return CliValuePrompt
			.getString(CliValuePrompt.getPromptableValue(cli, param, new ConsolePrompter(false, prompt, promptParams)));
	}

	public static final String getPasswordString(CommandLineValues cli, ParameterWrapper param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getPasswordString(cli, ParameterTools.unwrap(param), prompt, promptParams);
	}

	public static final char[] getPassword(CommandLineValues cli, ParameterWrapper param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getPassword(cli, ParameterTools.unwrap(param), prompt, promptParams);
	}

	public static final String getPasswordString(CommandLineValues cli, ParameterDefinition param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getString(CliValuePrompt.getPassword(cli, param, prompt, promptParams));
	}

	public static final char[] getPassword(CommandLineValues cli, ParameterDefinition param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getPromptableValue(cli, param, new ConsolePrompter(true, prompt, promptParams));
	}

	public static final char[] getPromptableValue(CommandLineValues cli, ParameterWrapper param,
		PromptCallback promptCallback) {
		return CliValuePrompt.getPromptableValue(cli, ParameterTools.unwrap(param), promptCallback);
	}

	public static final char[] getPromptableValue(CommandLineValues cli, ParameterDefinition param,
		PromptCallback promptCallback) {
		// If the parameter is given, return its value
		if ((cli != null) && (param != null)
			&& cli.isPresent(param)) { return CliValuePrompt.getChars(cli.getString(param)); }
		if (promptCallback == null) { return null; }
		return promptCallback.promptForValue();
	}
}