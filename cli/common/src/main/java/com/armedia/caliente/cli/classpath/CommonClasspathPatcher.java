package com.armedia.caliente.cli.classpath;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonClasspathPatcher extends ClasspathPatcher {

	protected static final String DEFAULT_CFG_DIR = "cfg";
	protected static final String DEFAULT_CFG_ENV_VAR = "CALIENTE_CFG";
	protected static final String DEFAULT_LIB_DIR = "caliente.lib";
	protected static final String DEFAULT_LIB_ENV_VAR = "CALIENTE_LIB";

	private static final FileFilter LIB_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if (!pathname.isFile()) { return false; }
			final String name = pathname.getName().toLowerCase();
			return name.endsWith(".zip") || name.endsWith(".jar");
		}
	};

	private final Logger log = LoggerFactory.getLogger(getClass());

	private File createFile(String path) {
		return createFile(null, path);
	}

	private File createFile(File parent, String path) {
		File f = (parent != null ? new File(parent, path) : new File(path));
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			this.log.warn(String.format("Failed to canonicalize the path for [%s]", f.getPath()), e);
		} finally {
			f = f.getAbsoluteFile();
		}
		return f;
	}

	protected String getCfgDirName() {
		return CommonClasspathPatcher.DEFAULT_CFG_DIR;
	}

	protected String getCfgDirEnvVarName() {
		return CommonClasspathPatcher.DEFAULT_CFG_ENV_VAR;
	}

	protected String getLibDirName() {
		return CommonClasspathPatcher.DEFAULT_LIB_DIR;
	}

	protected String getLibDirEnvVarName() {
		return CommonClasspathPatcher.DEFAULT_LIB_ENV_VAR;
	}

	protected Collection<URL> getPrePatches(boolean verbose) {
		return null;
	}

	@Override
	public final Collection<URL> getPatches(boolean verbose) throws Exception {
		Collection<URL> ret = new LinkedHashSet<>();
		{
			Collection<URL> pre = getPrePatches(verbose);
			if (pre != null) {
				ret.addAll(pre);
			}
		}
		String cfgDir = getCfgDirName();
		if (cfgDir == null) {
			String envVar = getCfgDirEnvVarName();
			if (envVar != null) {
				cfgDir = System.getenv(envVar);
			}
		}

		if (cfgDir != null) {
			File f = createFile(cfgDir);
			if (f.isDirectory() && f.canRead()) {
				ret.add(f.toURI().toURL());
			}
		}

		// Next, identify the DOCUMENTUM_EXTRA location, and all the JAR and ZIP files in there
		// (non-recursive), including a "classes" directory
		String libDir = getLibDirName();
		if (libDir == null) {
			String envVar = getLibDirEnvVarName();
			if (envVar != null) {
				libDir = System.getenv(envVar);
			}
		}
		if (libDir != null) {
			// Next, is it a directory?
			File f = createFile(libDir);
			if (f.isDirectory() && f.canRead()) {
				// Ok so we have the directory...does "classes" exist?
				File classesDir = createFile(f, "classes");
				if (classesDir.exists() && classesDir.isDirectory() && classesDir.canRead()) {
					ret.add(classesDir.toURI().toURL());
				}

				// Make sure they're sorted by name
				Map<String, URL> urls = new TreeMap<>();
				for (File jar : f.listFiles(CommonClasspathPatcher.LIB_FILTER)) {
					urls.put(jar.getName(), jar.toURI().toURL());
				}
				for (String s : urls.keySet()) {
					ret.add(urls.get(s));
				}
			}
		}
		postPatch(ret, verbose);
		return ret;
	}

	protected void postPatch(Collection<URL> patches, boolean verbose) {
	}
}