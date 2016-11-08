package com.armedia.caliente.filenamemapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PluggableServiceLocator;

public class Launcher {
	protected static final String DFC_PROPERTIES_PROP = "dfc.properties.file";

	private static final Logger log = LoggerFactory.getLogger(Launcher.class);

	public static final void main(String... args) {
		System.exit(Launcher.runMain(args));
	}

	private static int runMain(String... args) {
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return 1;
		}
		Set<URL> patches = new LinkedHashSet<URL>();
		PluggableServiceLocator<ClasspathPatcher> patchers = new PluggableServiceLocator<ClasspathPatcher>(
			ClasspathPatcher.class);
		patchers.setHideErrors(false);
		for (ClasspathPatcher p : patchers) {
			List<URL> l = p.getPatches();
			if ((l == null) || l.isEmpty()) {
				continue;
			}
			for (URL u : l) {
				if (u != null) {
					patches.add(u);
				}
			}
		}

		for (URL u : patches) {
			try {
				ClasspathPatcher.addToClassPath(u);
			} catch (IOException e) {
				Launcher.log.error("Failed to configure the dynamic classpath", e);
				return 1;
			}
			Launcher.log.info(String.format("Classpath addition: [%s]", u));
		}

		// final boolean debug = CLIParam.debug.isPresent();

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
				Launcher.log.warn("The DFC properties file [{}] {} - will continue using DFC defaults",
					f.getAbsolutePath(), error);
			}
		}

		try {
			return FilenameMapper.run();
		} catch (Exception e) {
			Launcher.log.error("Exception caught", e);
			return 1;
		}
	}
}