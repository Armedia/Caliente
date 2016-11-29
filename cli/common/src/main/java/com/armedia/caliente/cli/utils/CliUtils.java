package com.armedia.caliente.cli.utils;

import java.io.Console;
import java.io.File;
import java.io.IOException;

import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.Parameter;
import com.armedia.caliente.cli.parser.ParameterTools;
import com.armedia.caliente.cli.parser.ParameterWrapper;

public class CliUtils {

	static File newFileObject(String path) {
		return CliUtils.newFileObject(null, path);
	}

	static File newFileObject(File parent, String path) {
		File f = (parent != null ? new File(parent, path) : new File(path));
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// this.log.warn(String.format("Failed to canonicalize the path for [%s]",
			// f.getAbsolutePath()), e);
			// Do nothing, for now
		} finally {
			f = f.getAbsoluteFile();
		}
		return f;
	}

	public static final String getPassword(CommandLineValues cli, ParameterWrapper param, String prompt,
		Object... promptParams) {
		return CliUtils.getPassword(cli, ParameterTools.unwrap(param), prompt, promptParams);
	}

	public static final String getPassword(CommandLineValues cli, Parameter param, String prompt,
		Object... promptParams) {
		// If the parameter is given, return its value
		if ((cli != null) && (param != null) && cli.isPresent(param)) { return cli.getString(param); }

		final Console console = System.console();
		if (console == null) { return null; }

		// If the parameter isn't given, but a console is available, ask for a password
		// interactively
		if (prompt == null) {
			prompt = "Password:";
		}
		char[] pass = console.readPassword(prompt, promptParams);
		if (pass != null) { return new String(pass); }
		return null;
	}
}