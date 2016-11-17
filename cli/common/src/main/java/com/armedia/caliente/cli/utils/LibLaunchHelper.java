package com.armedia.caliente.cli.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchParameterSet;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.MutableParameter;
import com.armedia.caliente.cli.parser.Parameter;

public final class LibLaunchHelper implements LaunchParameterSet, LaunchClasspathHelper {

	private static final Parameter LIB = new MutableParameter() //
		.setShortOpt('l') //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("directory") //
		.setDescription(
			"The directory which contains extra classes (JARs, ZIPs or a classes directory) that should be added to the classpath") //
		.freezeCopy();

	private static final FileFilter LIB_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if (!pathname.isFile()) { return false; }
			final String name = pathname.getName().toLowerCase();
			return name.endsWith(".zip") || name.endsWith(".jar");
		}
	};

	public static final String DEFAULT_LIB = "caliente.lib";
	public static final String LIB_ENV_VAR = "CALIENTE_LIB";

	private final String defaultLib;
	private final String libEnvVar;

	public LibLaunchHelper() {
		this(LibLaunchHelper.DEFAULT_LIB, LibLaunchHelper.LIB_ENV_VAR);
	}

	public LibLaunchHelper(String defaultLib) {
		this(defaultLib, LibLaunchHelper.LIB_ENV_VAR);
	}

	public LibLaunchHelper(String defaultLib, String libEnvVar) {
		this.defaultLib = defaultLib;
		this.libEnvVar = libEnvVar;
	}

	@Override
	public Collection<? extends Parameter> getParameterDefinitions(CommandLineValues commandLine) {
		return Collections.singleton(LibLaunchHelper.LIB);
	}

	@Override
	public Collection<URL> getClasspathPatchesPre(CommandLineValues cli) {
		List<URL> ret = new ArrayList<>();
		try {
			// First...use the command-line parameter given, and apply any defaults
			// we may be configured for
			String var = cli.getString(LibLaunchHelper.LIB, this.defaultLib);
			if (var == null) {
				// No command-line parameter given...if we have an environment variable configured,
				// given, we use that
				if (this.libEnvVar != null) {
					var = System.getenv(this.libEnvVar);
				}
			}

			// If we had no lib parameter, nor an environment variable to fall back upon, there's
			// nothing else to be done
			if (var == null) { return null; }

			// We got something!! Inspect it...
			// Next, is it a directory?
			File f = Utils.newFileObject(var);
			if (f.isDirectory() && f.canRead()) {
				// Ok so we have the directory...does "classes" exist?
				File classesDir = Utils.newFileObject(f, "classes");
				if (classesDir.exists() && classesDir.isDirectory() && classesDir.canRead()) {
					ret.add(classesDir.toURI().toURL());
				}

				// Make sure they're sorted by name
				Map<String, URL> urls = new TreeMap<>();
				for (File jar : f.listFiles(LibLaunchHelper.LIB_FILTER)) {
					urls.put(jar.getName(), jar.toURI().toURL());
				}
				for (String s : urls.keySet()) {
					ret.add(urls.get(s));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to configure the dynamic library classpath for %s"), e);
		}
		return ret;
	}

	@Override
	public Collection<URL> getClasspathPatchesPost(CommandLineValues commandLine) {
		return null;
	}

	@Override
	public String toString() {
		return String.format("LibLaunchHelper [defaultLib=%s, libEnvVar=%s]", this.defaultLib, this.libEnvVar);
	}
}