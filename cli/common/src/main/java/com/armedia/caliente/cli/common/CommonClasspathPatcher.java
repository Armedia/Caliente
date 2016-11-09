package com.armedia.caliente.cli.common;

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

public abstract class CommonClasspathPatcher extends ClasspathPatcher {

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
		return "cfg";
	}

	protected String getCfgDirEnvVarName() {
		return "CALIENTE_CFG";
	}

	protected String getLibDirName() {
		return "lib";
	}

	protected String getLibDirEnvVarName() {
		return "CALIENTE_LIB";
	}

	protected void prePatch(List<URL> patches) {
	}

	@Override
	public final List<URL> getPatches() {
		List<URL> ret = new ArrayList<>(3);
		prePatch(ret);
		try {
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
		} catch (IOException e) {
			throw new RuntimeException("Failed to configure the dynamic classpath", e);
		}
		postPatch(ret);
		return ret;
	}

	protected void postPatch(List<URL> patches) {
	}
}