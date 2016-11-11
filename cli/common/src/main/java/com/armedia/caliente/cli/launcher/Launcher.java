package com.armedia.caliente.cli.launcher;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.classpath.ClasspathPatcher.Filter;
import com.armedia.caliente.cli.parser.CommandLine;
import com.armedia.caliente.cli.parser.CommandLineParseException;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public abstract class Launcher<K> {
	protected final Logger log = LoggerFactory.getLogger(Launcher.class);

	/**
	 * <p>
	 * Return a {@link Filter} instance that will be used when selecting which
	 * {@link ClasspathPatcher} instances are employed during the patching stage.
	 * </p>
	 *
	 * @return the {@link Filter} instance to use
	 */
	protected ClasspathPatcher.Filter getClasspathPatcherFilter(CommandLineValues commandLine) {
		return null;
	}

	/**
	 * <p>
	 * Returns the parameters definitions to be used in the given pass. If {@code null} is returned,
	 * or an empty {@link Collection}, then the parsing cycle will be broken and no further command
	 * parameter parsing will be performed. The {@code pass} argument is guaranteed to always be
	 * increasing.
	 * </p>
	 *
	 * @param commandLine
	 * @param pass
	 * @return the collection of {@link ParameterDefinition} instances to use in the next parsing
	 *         pass
	 */
	protected abstract Collection<ParameterDefinition> getCommandLineParameters(CommandLineValues commandLine,
		int pass);

	/**
	 * <p>
	 * Returns the name to be given to this executable after the given command parsing pass. The
	 * {@code pass} argument is guaranteed to always be increasing.
	 * </p>
	 *
	 * @param pass
	 * @return the name to be used for this executable
	 */
	protected abstract String getProgramName(int pass);

	protected final int launch(String... args) {
		return launch(true, args);
	}

	protected final int launch(boolean supportsHelp, String... args) {

		// This loop subclasses a chance to cleanly break the parameter parsing loop, while
		// also affording them the opportunity to modify the parameter availability based on
		// previously parsed parameters
		CommandLine cl = new CommandLine(supportsHelp);
		int pass = -1;
		while (true) {
			Collection<ParameterDefinition> parameters = getCommandLineParameters(cl, ++pass);
			if ((parameters == null) || parameters.isEmpty()) {
				break;
			}

			for (ParameterDefinition def : parameters) {
				try {
					cl.define(def);
				} catch (Exception e) {
					throw new RuntimeException("Failed to initialize the command-line parser", e);
				}
			}
			try {
				cl.parse(getProgramName(pass), args);
				if (cl.isHelpRequested()) {
					System.err.printf("%s%n", cl.getHelpMessage());
					return 1;
				}
			} catch (CommandLineParseException e) {
				if (e.getHelp() != null) {
					System.err.printf("%s%n", e.getHelp());
				}
				return 1;
			}
		}

		if (ClasspathPatcher.discoverPatches(getClasspathPatcherFilter(cl), false)) {
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