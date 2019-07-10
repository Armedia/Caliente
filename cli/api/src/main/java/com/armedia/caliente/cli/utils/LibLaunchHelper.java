/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.armedia.commons.utilities.Tools;

public final class LibLaunchHelper extends Options implements LaunchClasspathHelper {

	public static final Option LIB = new OptionImpl() //
		.setShortOpt('l') //
		.setLongOpt("lib") //
		.setMinArguments(1) //
		.setMaxArguments(-1) //
		.setArgumentName("directory-or-file") //
		.setDescription(
			"A JAR/ZIP library file, or a directory which contains extra classes (JARs, ZIPs or a classes directory) that should be added to the classpath") //
	;

	private static final DirectoryStream.Filter<Path> LIB_FILTER = (path) -> {
		if (!Files.isRegularFile(path)) { return false; }
		final String name = StringUtils.lowerCase(path.getFileName().toString());
		return StringUtils.endsWith(name, ".zip") || StringUtils.endsWith(name, ".jar");
	};

	public static final String DEFAULT_LIB = "caliente.lib";
	public static final String LIB_ENV_VAR = "CALIENTE_LIB";

	private final String defaultLib;
	private final String envVarName;

	private final OptionGroupImpl group;

	public LibLaunchHelper() {
		this(LibLaunchHelper.DEFAULT_LIB, LibLaunchHelper.LIB_ENV_VAR);
	}

	public LibLaunchHelper(String defaultLib) {
		this(defaultLib, LibLaunchHelper.LIB_ENV_VAR);
	}

	public LibLaunchHelper(String defaultLib, String libEnvVar) {
		this.defaultLib = defaultLib;
		this.envVarName = libEnvVar;
		this.group = new OptionGroupImpl("Classpath Extender") //
			.add(LibLaunchHelper.LIB) //
		;
	}

	public final String getDefault() {
		return this.defaultLib;
	}

	public final String getEnvVarName() {
		return this.envVarName;
	}

	protected boolean isViableLibrary(Path path) {
		if (!Files.isRegularFile(path)) { return false; }
		// TODO: Do we want to perform additional validation? I.e. check the contents to
		// see if it's really a JAR file
		/*
		try (ZipFile zf = new ZipFile(path.toFile())) {
			zf.size();
		} catch (ZipException e) {
			return false;
		}
		*/
		return true;
	}

	protected void collectEntries(String path, List<URL> ret) throws IOException {
		if (StringUtils.isEmpty(path)) { return; }
		File f = CliUtils.newFileObject(path);
		if (!f.exists()) { return; }
		if (!f.canRead()) { return; }

		if (f.isFile()) {
			if (isViableLibrary(f.toPath())) {
				ret.add(f.toURI().toURL());
			}
			return;
		}

		if (f.isDirectory()) {
			// Ok so we have the directory...does "classes" exist?
			File classesDir = CliUtils.newFileObject(f, "classes");
			if (classesDir.exists() && classesDir.isDirectory() && classesDir.canRead()) {
				ret.add(classesDir.toURI().toURL());
			}

			// Make sure they're sorted by name
			Map<String, URL> urls = new TreeMap<>();
			Files.newDirectoryStream(f.toPath(), LibLaunchHelper.LIB_FILTER).forEach((jar) -> {
				if (isViableLibrary(jar)) {
					try {
						urls.put(jar.getFileName().toString(), jar.toUri().toURL());
					} catch (MalformedURLException e) {
						throw new RuntimeException(String.format("Failed to convert the path [%s] to a URL", jar), e);
					}
				}
			});
			urls.values().stream().forEach(ret::add);
		}
	}

	@Override
	public Collection<URL> getClasspathPatchesPre(OptionValues cli) {
		List<URL> ret = new ArrayList<>();
		String currentPath = null;
		try {
			if (!cli.isPresent(LibLaunchHelper.LIB)) {
				// No option given, use the default or environment variable
				Collection<String> paths = Collections.emptyList();
				if (this.envVarName != null) {
					// Use the environment variable
					String str = System.getenv(this.envVarName);
					if (str != null) {
						paths = Tools.splitEscaped(File.pathSeparatorChar, str);
					}
				} else if (this.defaultLib != null) {
					// Use the configured default
					paths = Collections.singleton(this.defaultLib);
				}

				if (paths != null) {
					for (String path : paths) {
						collectEntries(currentPath = path, ret);
					}
				}
				return ret;
			}

			for (String path : cli.getStrings(LibLaunchHelper.LIB)) {
				collectEntries(currentPath = path, ret);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(
				String.format("Failed to configure the dynamic library classpath from [%s]", currentPath), e);
		}
		return ret;
	}

	@Override
	public Collection<URL> getClasspathPatchesPost(OptionValues commandLine) {
		return null;
	}

	@Override
	public String toString() {
		return String.format("LibLaunchHelper [defaultLib=%s, libEnvVar=%s]", this.defaultLib, this.envVarName);
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group.getCopy(name);
	}
}