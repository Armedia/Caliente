package com.armedia.caliente.cli.utils;

import java.io.Console;

import org.apache.commons.lang3.ArrayUtils;

import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.Parameter;
import com.armedia.caliente.cli.parser.ParameterTools;
import com.armedia.caliente.cli.parser.ParameterWrapper;
import com.armedia.commons.utilities.Tools;

public class CliValuePrompt {

	public interface ValueCallback {
		public String getValue();
	}

	public static class ConsoleReader implements ValueCallback {
		private final boolean silent;
		private final String prompt;
		private final Object[] promptParams;

		public ConsoleReader(boolean silent, String prompt, Object... promptParams) {
			this.silent = silent;
			this.prompt = Tools.coalesce(prompt, "Password:");
			if (promptParams != null) {
				this.promptParams = promptParams.clone();
			} else {
				this.promptParams = ArrayUtils.EMPTY_OBJECT_ARRAY;
			}
		}

		@Override
		public String getValue() {
			final Console console = System.console();
			if (console == null) { return null; }

			// If the parameter isn't given, but a console is available, ask for a password
			// interactively
			if (!this.silent) { return console.readLine(this.prompt, this.promptParams); }
			char[] pass = console.readPassword(this.prompt, this.promptParams);
			if (pass != null) { return new String(pass); }
			return null;
		}
	}

	public static final String getUsername(CommandLineValues cli, ParameterWrapper param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getValue(cli, ParameterTools.unwrap(param), false, prompt, promptParams);
	}

	public static final String getUsername(CommandLineValues cli, Parameter param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getValue(cli, param, new ConsoleReader(false, prompt, promptParams));
	}

	public static final String getPassword(CommandLineValues cli, ParameterWrapper param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getValue(cli, param, true, prompt, promptParams);
	}

	public static final String getPassword(CommandLineValues cli, Parameter param, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getValue(cli, param, true, prompt, promptParams);
	}

	public static final String getValue(CommandLineValues cli, ParameterWrapper param, boolean silent, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getValue(cli, ParameterTools.unwrap(param), silent, prompt, promptParams);
	}

	public static final String getValue(CommandLineValues cli, Parameter param, boolean silent, String prompt,
		Object... promptParams) {
		return CliValuePrompt.getValue(cli, param, new ConsoleReader(silent, prompt, promptParams));
	}

	public static final String getValue(CommandLineValues cli, ParameterWrapper param, ValueCallback valueCallback) {
		return CliValuePrompt.getValue(cli, ParameterTools.unwrap(param), valueCallback);
	}

	public static final String getValue(CommandLineValues cli, Parameter param, ValueCallback valueCallback) {
		// If the parameter is given, return its value
		if ((cli != null) && (param != null) && cli.isPresent(param)) { return cli.getString(param); }
		if (valueCallback == null) { return null; }
		return valueCallback.getValue();
	}
}