package com.armedia.caliente.cli.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.Options;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;

public final class LibLaunchHelper extends Options implements LaunchClasspathHelper {

	public static final Option LIB = new OptionImpl() //
		.setShortOpt('l') //
		.setLongOpt("lib") //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("directory") //
		.setDescription(
			"The directory which contains extra classes (JARs, ZIPs or a classes directory) that should be added to the classpath") //
	;

	private static final DirectoryStream.Filter<Path> LIB_FILTER = (path) -> {
		if (!Files.isRegularFile(path)) { return false; }
		final String name = StringUtils.lowerCase(path.getFileName().toString());
		return StringUtils.endsWith(name, ".zip") || StringUtils.endsWith(name, ".jar");
	};

	public static final String DEFAULT_LIB = "caliente.lib";
	public static final String LIB_ENV_VAR = "CALIENTE_LIB";

	private final String defaultLib;
	private final String libEnvVar;

	private final OptionGroupImpl group;

	public LibLaunchHelper() {
		this(LibLaunchHelper.DEFAULT_LIB, LibLaunchHelper.LIB_ENV_VAR);
	}

	public LibLaunchHelper(String defaultLib) {
		this(defaultLib, LibLaunchHelper.LIB_ENV_VAR);
	}

	public LibLaunchHelper(String defaultLib, String libEnvVar) {
		this.defaultLib = defaultLib;
		this.libEnvVar = libEnvVar;
		this.group = new OptionGroupImpl("Classpath Extender") //
			.add(LibLaunchHelper.LIB) //
		;
	}

	@Override
	public Collection<URL> getClasspathPatchesPre(OptionValues cli) {
		List<URL> ret = new ArrayList<>();
		try {
			// First...use the command-line option given, and apply any defaults
			// we may be configured for
			String var = cli.getString(LibLaunchHelper.LIB, this.defaultLib);
			if (var == null) {
				// No command-line option given...if we have an environment variable configured,
				// given, we use that
				if (this.libEnvVar != null) {
					var = System.getenv(this.libEnvVar);
				}
			}

			// If we had no lib option, nor an environment variable to fall back upon, there's
			// nothing else to be done
			if (var == null) { return null; }

			// We got something!! Inspect it...
			// Next, is it a directory?
			File f = CliUtils.newFileObject(var);
			if (f.isDirectory() && f.canRead()) {
				// Ok so we have the directory...does "classes" exist?
				File classesDir = CliUtils.newFileObject(f, "classes");
				if (classesDir.exists() && classesDir.isDirectory() && classesDir.canRead()) {
					ret.add(classesDir.toURI().toURL());
				}

				// Make sure they're sorted by name
				Map<String, URL> urls = new TreeMap<>();
				Files.newDirectoryStream(f.toPath(), LibLaunchHelper.LIB_FILTER).forEach((jar) -> {
					try {
						urls.put(jar.getFileName().toString(), jar.toUri().toURL());
					} catch (MalformedURLException e) {
						throw new RuntimeException(String.format("Failed to convert the path [%s] to a URL", jar), e);
					}
				});
				urls.values().stream().forEachOrdered(ret::add);
			}
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to configure the dynamic library classpath for %s"), e);
		}
		return ret;
	}

	@Override
	public Collection<URL> getClasspathPatchesPost(OptionValues commandLine) {
		return null;
	}

	@Override
	public String toString() {
		return String.format("LibLaunchHelper [defaultLib=%s, libEnvVar=%s]", this.defaultLib, this.libEnvVar);
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group.getCopy(name);
	}
}