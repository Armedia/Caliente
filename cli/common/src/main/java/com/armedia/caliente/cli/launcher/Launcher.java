package com.armedia.caliente.cli.launcher;

import java.util.Collection;
import java.util.Collections;

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

	protected void processCommandLine(CommandLineValues commandLine) {
	}

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

	protected Collection<ClasspathPatcher> getClasspathPatchersPre(CommandLineValues commandLine) {
		return Collections.emptyList();
	}

	protected Collection<ClasspathPatcher> getClasspathPatchersPost(CommandLineValues commandLine) {
		return Collections.emptyList();
	}

	protected final int launch(String... args) {
		return launch(true, args);
	}

	protected void initLogging(CommandLineValues cl) {
		// By default, do nothing...
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

		// Process the parameters given...
		processCommandLine(cl);

		Collection<ClasspathPatcher> extraPatchers = getClasspathPatchersPre(cl);
		if (extraPatchers != null) {
			for (ClasspathPatcher p : extraPatchers) {
				if (p != null) {
					try {
						p.applyPatches(false);
					} catch (Exception e) {
						throw new RuntimeException("Failed to apply the a-poriori classpath patches", e);
					}
				}
			}
		}
		ClasspathPatcher.discoverPatches(getClasspathPatcherFilter(cl), false);
		extraPatchers = getClasspathPatchersPost(cl);
		if (extraPatchers != null) {
			for (ClasspathPatcher p : extraPatchers) {
				if (p != null) {
					try {
						p.applyPatches(false);
					} catch (Exception e) {
						throw new RuntimeException("Failed to apply the a-posteriori classpath patches", e);
					}
				}
			}
		}

		initLogging(cl);

		for (String s : ClasspathPatcher.getAdditions()) {
			this.log.info("Classpath addition: [{}]", s);
		}

		try {
			return run(cl);
		} catch (Exception e) {
			this.log.error("Exception caught", e);
			return 1;
		}
	}

	protected abstract int run(CommandLineValues commandLine) throws Exception;
}