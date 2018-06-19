package com.armedia.caliente.cli.caliente.launcher;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.utils.ClasspathPatcher;
import com.armedia.commons.utilities.Tools;

public class CommonClasspathPatcher extends ClasspathPatcher {

	private static final String ENV_CALIENTE_LIB = "CALIENTE_LIB";
	private static final String DEFAULT_CALIENTE_LIB = "caliente.lib";

	private static final FileFilter LIB_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if (!pathname.isFile()) { return false; }
			final String name = pathname.getName().toLowerCase();
			return name.endsWith(".zip") || name.endsWith(".jar");
		}
	};

	private final Logger log = LoggerFactory.getLogger(getClass());

	private File constructFile(String path) {
		return constructFile(null, path);
	}

	private File constructFile(File parent, String path) {
		File f = (parent != null ? new File(parent, path) : new File(path));
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			f = f.getAbsoluteFile();
			this.log.warn(String.format("Failed to canonicalize the path for [%s]", f.getAbsolutePath()), e);
		}
		return f;
	}

	@Override
	public boolean supportsEngine(String engine) {
		return (engine != null);
	}

	@Override
	public List<URL> getPatches(String engine) {
		List<URL> ret = new ArrayList<>(3);
		try {
			File f = constructFile(System.getProperty("user.dir"));
			ret.add(constructFile(f, "cfg").toURI().toURL());

			// Next, identify the DOCUMENTUM_EXTRA location, and all the JAR and ZIP files in there
			// (non-recursive), including a "classes" directory
			String var = CLIParam.lib.getString(Tools.coalesce(System.getenv(CommonClasspathPatcher.ENV_CALIENTE_LIB),
				CommonClasspathPatcher.DEFAULT_CALIENTE_LIB));
			// Next, is it a directory?
			f = constructFile(var);
			if (f.isDirectory() && f.canRead()) {
				// Ok so we have the directory...does "classes" exist?
				File classesDir = constructFile(f, "classes");
				if (classesDir.exists() && classesDir.isDirectory() && classesDir.canRead()) {
					ret.add(classesDir.toURI().toURL());
				}

				// Make sure they're sorted by title
				Map<String, URL> urls = new TreeMap<>();
				for (File jar : f.listFiles(CommonClasspathPatcher.LIB_FILTER)) {
					urls.put(jar.getName(), jar.toURI().toURL());
				}
				for (String s : urls.keySet()) {
					ret.add(urls.get(s));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(
				String.format("Failed to configure the dynamic classpath for engine [%s]", engine), e);
		}
		return ret;
	}
}