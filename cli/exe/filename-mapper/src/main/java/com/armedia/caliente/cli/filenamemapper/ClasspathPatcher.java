package com.armedia.caliente.cli.filenamemapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ClasspathPatcher {

	private static final Class<?>[] PARAMETERS = new Class[] {
		URL.class
	};
	private static final URLClassLoader CL;
	private static final Method METHOD;
	private static final Set<String> ADDED = new HashSet<>();

	static {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		if (!(cl instanceof URLClassLoader)) {
			throw new RuntimeException("System Classloader is not a URLClassLoader");
		}
		CL = URLClassLoader.class.cast(cl);
		try {
			METHOD = URLClassLoader.class.getDeclaredMethod("addURL", ClasspathPatcher.PARAMETERS);
			ClasspathPatcher.METHOD.setAccessible(true);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to initialize access to the addURL() method in the system classloader",
				t);
		}
	}

	public static synchronized void addToClassPath(URL u) throws IOException {
		if (u == null) { throw new IllegalArgumentException("Must provide a URL to add to the classpath"); }
		if (ClasspathPatcher.ADDED.contains(u.toString())) { return; }
		try {
			ClasspathPatcher.METHOD.invoke(ClasspathPatcher.CL, u);
			ClasspathPatcher.ADDED.add(u.toString());
		} catch (Throwable t) {
			throw new IOException(String.format("Failed to add the URL [%s] to the system classloader", u), t);
		}
	}

	public static synchronized void addToClassPath(File f) throws IOException {
		if (f == null) { throw new IllegalArgumentException("Must provide a File to add to the classpath"); }
		ClasspathPatcher.addToClassPath(f.toURI().toURL());
	}

	public static synchronized void addToClassPath(String f) throws IOException {
		if (f == null) { throw new IllegalArgumentException("Must provide a File path to add to the classpath"); }
		ClasspathPatcher.addToClassPath(new File(f));
	}

	protected ClasspathPatcher() {
	}

	public abstract List<URL> getPatches();
}