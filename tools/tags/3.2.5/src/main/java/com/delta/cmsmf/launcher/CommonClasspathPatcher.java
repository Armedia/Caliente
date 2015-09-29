package com.delta.cmsmf.launcher;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.delta.cmsmf.utils.ClasspathPatcher;

public class CommonClasspathPatcher extends ClasspathPatcher {

	private static final FilenameFilter LIB_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			name = name.toLowerCase();
			return name.endsWith(".jar") || name.endsWith(".zip");
		}
	};

	public CommonClasspathPatcher() {
		super();
	}

	@Override
	public boolean supportsEngine(String engine) {
		return (engine != null);
	}

	@Override
	public List<URL> getPatches(String engine) {
		List<URL> ret = new ArrayList<URL>(3);
		try {
			// First, add the ${PWD}/cfg directory to the classpath - whether it exists or not
			final File userDir = new File(System.getProperty("user.dir"));

			// Add all jar/zip files in ${user.dir}/cmsmf.lib, and ${user.dir}/cmsmf.lib/classes
			// to the classpath
			File libDir = new File(userDir, "cmsmf.lib");
			if (libDir.exists() && libDir.isDirectory()) {
				ret.add(new File(libDir, "classes").toURI().toURL());
				for (File f : libDir.listFiles(CommonClasspathPatcher.LIB_FILTER)) {
					ret.add(f.toURI().toURL());
				}
			}
			ret.add(new File(userDir, "cfg").toURI().toURL());
			ret.add(userDir.toURI().toURL());
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to configure the dynamic classpath for engine [%s]",
				engine), e);
		}
		return ret;
	}
}