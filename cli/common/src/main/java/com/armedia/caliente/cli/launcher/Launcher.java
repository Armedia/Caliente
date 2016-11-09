package com.armedia.caliente.cli.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.classpath.ClasspathPatcher;

public abstract class Launcher {
	protected final Logger log = LoggerFactory.getLogger(Launcher.class);

	protected ClasspathPatcher.Filter getClasspathPatcherFilter() {
		return null;
	}

	protected final int launch(String... args) {
		/*
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return 1;
		}
		*/

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