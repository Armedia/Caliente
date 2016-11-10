package com.armedia.caliente.cli.launcher;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.parser.CommandLine;
import com.armedia.caliente.cli.parser.CommandLineParseException;
import com.armedia.caliente.cli.parser.HelpRequestedException;
import com.armedia.caliente.cli.parser.Parameter;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public abstract class Launcher<K> {
	protected final Logger log = LoggerFactory.getLogger(Launcher.class);

	protected boolean supportsHelp = true;

	protected ClasspathPatcher.Filter getClasspathPatcherFilter() {
		return null;
	}

	protected abstract Map<K, ParameterDefinition> getCommandLineParameters();

	protected abstract void parameterDefined(K key, Parameter param);

	protected abstract String getProgramName();

	protected final int launch(String... args) {
		CommandLine cl = new CommandLine(this.supportsHelp);
		Map<K, ParameterDefinition> parameters = getCommandLineParameters();
		if (parameters != null) {
			for (final K key : parameters.keySet()) {
				final ParameterDefinition def = parameters.get(key);
				try {
					parameterDefined(key, cl.define(def));
				} catch (Exception e) {
					throw new RuntimeException("Failed to initialize the command-line parser", e);
				}
			}
		}

		try {
			cl.parse(getProgramName(), args);
		} catch (CommandLineParseException e) {
			if (e.getHelp() != null) {
				System.err.printf("%s%n", e.getHelp());
			}
			return 1;
		} catch (HelpRequestedException e) {
			System.err.printf("%s%n", e.getMessage());
			return 1;
		}

		if (ClasspathPatcher.discoverPatches(getClasspathPatcherFilter(), false)) {
			for (String s : ClasspathPatcher.getAdditions()) {
				this.log.info("Classpath addition: [{}]", s);
			}
		}

		// final boolean debug = CLIParam.debug.isPresent();
		/*
		if (CLIParam.dfc_prop.isPresent()) {
			File f = new File(CLIParam.dfc_prop.getString("dfc.properties"));
			try {
				f = f.getCanonicalFile();
			} catch (IOException e) {
				// Do nothing...stay with the non-canonical path
				f = f.getAbsoluteFile();
			}
			String error = null;
			if ((error == null) && !f.exists()) {
				error = "does not exist";
			}
			if ((error == null) && !f.isFile()) {
				error = "is not a regular file";
			}
			if ((error == null) && !f.canRead()) {
				error = "cannot be read";
			}
			if (error == null) {
				System.setProperty(Launcher.DFC_PROPERTIES_PROP, f.getAbsolutePath());
			} else {
				this.log.warn("The DFC properties file [{}] {} - will continue using DFC defaults", f.getAbsolutePath(),
					error);
			}
		}
		*/

		try {
			return run();
		} catch (Exception e) {
			this.log.error("Exception caught", e);
			return 1;
		}
	}

	protected abstract int run() throws Exception;
}