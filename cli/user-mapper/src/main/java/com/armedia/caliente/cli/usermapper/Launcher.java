package com.armedia.caliente.cli.usermapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public class Launcher extends AbstractLauncher {
	protected static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	protected static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	protected static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	protected static final String DCTM_JAR = "dctm.jar";
	protected static final String DFC_TEST_CLASS = "com.documentum.fc.client.IDfFolder";

	private File newFileObject(String path) {
		return newFileObject(null, path);
	}

	private File newFileObject(File parent, String path) {
		File f = (parent != null ? new File(parent, path) : new File(path));
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			this.log.warn(String.format("Failed to canonicalize the path for [%s]", f.getAbsolutePath()), e);
		} finally {
			f = f.getAbsoluteFile();
		}
		return f;
	}

	@Override
	protected Collection<URL> getClasspathPatchesPre(CommandLineValues cli) {
		final boolean dfcFound;
		{
			boolean dfc = false;
			try {
				Class.forName(Launcher.DFC_TEST_CLASS);
				dfc = true;
			} catch (Exception e) {
				dfc = false;
			}
			dfcFound = dfc;
		}

		List<URL> ret = new ArrayList<>(3);
		try {
			// Even if not configured, if there's a dfc.properties in our current
			// working directory, we try to use it
			String var = cli.getString(CLIParam.dfc_prop, "dfc.properties");
			if (var != null) {
				File f = newFileObject(var);
				if (f.exists() && f.isFile() && f.canRead()) {
					System.setProperty(Launcher.DFC_PROPERTIES_PROP, f.getAbsolutePath());
				}
			}

			// Next, add ${DOCUMENTUM}/config to the classpath
			var = cli.getString(CLIParam.dctm, System.getenv(Launcher.ENV_DOCUMENTUM));
			// Go with the environment
			if (var == null) {
				String msg = String.format("The environment variable [%s] is not set", Launcher.ENV_DOCUMENTUM);
				if (!dfcFound) { throw new RuntimeException(msg); }
				this.log.warn("{}, integrated DFC may encounter errors", msg);
			}

			if (var != null) {
				File f = newFileObject(var);
				if (!f.exists()) {
					FileUtils.forceMkdir(f);
				}
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the directory [%s]", f.getAbsolutePath())); }

				ret.add(newFileObject(f, "config").toURI().toURL());
			}

			// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
			var = cli.getString(CLIParam.dfc, System.getenv(Launcher.ENV_DOCUMENTUM_SHARED));
			// Go with the environment
			if (var == null) {
				String msg = String.format("The environment variable [%s] is not set", Launcher.ENV_DOCUMENTUM_SHARED);
				if (!dfcFound) { throw new RuntimeException(msg); }
				this.log.warn("{}, integrated DFC may encounter errors", msg);
			}

			if (var != null) {
				// Next, is it a directory?
				File f = newFileObject(var);
				if (!f.isDirectory()) { throw new FileNotFoundException(String.format(
					"Could not find the [%s] directory [%s]", Launcher.ENV_DOCUMENTUM_SHARED, f.getAbsolutePath())); }

				// Next, does dctm.jar exist in there?
				if (!dfcFound) {
					File tgt = newFileObject(f, Launcher.DCTM_JAR);
					if (!tgt.isFile()) { throw new FileNotFoundException(
						String.format("Could not find the JAR file [%s]", tgt.getAbsolutePath())); }

					// Next, to the classpath
					ret.add(tgt.toURI().toURL());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to configure the dynamic classpath", e);
		}
		return ret;
	}

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	@Override
	protected Collection<? extends ParameterDefinition> getCommandLineParameters(CommandLineValues commandLine,
		int pass) {
		if (pass > 0) { return null; }
		return Arrays.asList(CLIParam.values());
	}

	@Override
	protected int processCommandLine(CommandLineValues commandLine) {
		return super.processCommandLine(commandLine);
	}

	@Override
	protected String getProgramName(int pass) {
		return "Caliente User Mapper";
	}

	@Override
	protected int run(CommandLineValues cli) throws Exception {
		// final boolean debug = cli.isPresent(CLIParam.debug);

		if (cli.isPresent(CLIParam.dfc_prop)) {
			File f = new File(cli.getString(CLIParam.dfc_prop, "dfc.properties"));
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

		return new UserMapper(cli).run();
	}
}