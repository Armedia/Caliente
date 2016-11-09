package com.armedia.caliente.cli.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class ClasspathPatcher {

	public static interface Filter {
		public boolean include(ClasspathPatcher patcher);
	}

	private static final Filter ALL_FILTER = new Filter() {
		@Override
		public boolean include(ClasspathPatcher patcher) {
			return (patcher != null);
		}
	};

	private static final Class<?>[] PARAMETERS = new Class[] {
		URL.class
	};
	private static final URLClassLoader CL;
	private static final Method METHOD;
	private static final Set<String> ADDED = new LinkedHashSet<>();

	private final Logger log = LoggerFactory.getLogger(getClass());

	static {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		if (!(cl instanceof URLClassLoader)) { throw new RuntimeException(
			"System Classloader is not a URLClassLoader"); }
		CL = URLClassLoader.class.cast(cl);
		try {
			METHOD = URLClassLoader.class.getDeclaredMethod("addURL", ClasspathPatcher.PARAMETERS);
			ClasspathPatcher.METHOD.setAccessible(true);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to initialize access to the addURL() method in the system classloader",
				t);
		}
	}

	public ClasspathPatcher() {
	}

	public final boolean discoverPatches() {
		return discoverPatches(null, true);
	}

	public final boolean discoverPatches(Filter filter) {
		return discoverPatches(filter, true);
	}

	public final boolean discoverPatches(boolean quiet) {
		return discoverPatches(null, quiet);
	}

	public final synchronized boolean discoverPatches(Filter filter, boolean quiet) {
		if (filter == null) {
			filter = ClasspathPatcher.ALL_FILTER;
		}
		Set<URL> patches = new LinkedHashSet<>();
		PluggableServiceLocator<ClasspathPatcher> patchers = new PluggableServiceLocator<>(ClasspathPatcher.class);
		patchers.setHideErrors(false);
		for (ClasspathPatcher p : patchers) {
			// Make sure we only include the patchers we're interested in
			if (!filter.include(p)) {
				continue;
			}
			List<URL> l = null;
			try {
				l = p.getPatches();
			} catch (Exception e) {
				if (!quiet && this.log.isDebugEnabled()) {
					this.log.warn(String.format("Failed to load the classpath patches from [%s]",
						p.getClass().getCanonicalName()), e);
				}
				continue;
			}
			if ((l == null) || l.isEmpty()) {
				continue;
			}
			for (URL u : l) {
				if (u != null) {
					patches.add(u);
				}
			}
		}

		boolean ret = false;
		for (URL u : patches) {
			try {
				ret |= ClasspathPatcher.addToClassPath(u);
				if (!quiet) {
					this.log.info("Classpath addition: [{}]", u);
				}
			} catch (IOException e) {
				if (!quiet) {
					if (this.log.isDebugEnabled()) {
						this.log.error(String.format("Failed to add [%s] to the classpath", u), e);
					} else {
						this.log.error(String.format("Failed to add [%s] to the classpath", u));
					}
				}
			}
		}
		return ret;
	}

	public static synchronized Set<String> getAdditions() {
		return Tools.freezeSet(new LinkedHashSet<>(ClasspathPatcher.ADDED));
	}

	public static synchronized boolean addToClassPath(URL u) throws IOException {
		if (u == null) { throw new IllegalArgumentException("Must provide a URL to add to the classpath"); }
		if (ClasspathPatcher.ADDED.contains(u.toString())) { return false; }
		try {
			ClasspathPatcher.METHOD.invoke(ClasspathPatcher.CL, u);
			return ClasspathPatcher.ADDED.add(u.toString());
		} catch (Throwable t) {
			throw new IOException(String.format("Failed to add the URL [%s] to the system classloader", u), t);
		}
	}

	public static synchronized boolean addToClassPath(File f) throws IOException {
		if (f == null) { throw new IllegalArgumentException("Must provide a File to add to the classpath"); }
		return ClasspathPatcher.addToClassPath(f.toURI().toURL());
	}

	public static synchronized boolean addToClassPath(String f) throws IOException {
		if (f == null) { throw new IllegalArgumentException("Must provide a File path to add to the classpath"); }
		return ClasspathPatcher.addToClassPath(new File(f));
	}

	public abstract List<URL> getPatches() throws Exception;
}