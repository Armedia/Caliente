/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.cli.classpath;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.ShareableLockable;

public abstract class ClasspathPatcher {

	/**
	 * <p>
	 * This classloader exists as a substitute of {@link URLClassLoader} which we can freely invoke
	 * {@link URLClassLoader#addURL(URL)} on because it's no longer a protected method. This helps
	 * us sidestep some security issues with changing a method's visibility at runtime if that's not
	 * allowed. Other than this method scope change, it's identical to {@link URLClassLoader} in
	 * every way.
	 * </p>
	 */
	private static final class CPCL extends URLClassLoader {
		public CPCL(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
			super(urls, parent, factory);
		}

		public CPCL(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		public CPCL(URL[] urls) {
			super(urls);
		}

		@Override
		protected void addURL(URL url) {
			super.addURL(url);
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ClasspathPatcher.class);
	private static final URL[] NO_URLS = {};
	private static final URL NULL_URL = null;
	private static volatile ClassLoader CL = null;
	private static volatile Consumer<URL> ADD_URL = null;
	private static final Set<String> ADDED = new LinkedHashSet<>();
	private static final ShareableLockable LOCK = new BaseShareableLockable();

	static {
		ClasspathPatcher.init();
	}

	public static final ClassLoader init() {
		return ClasspathPatcher.LOCK.shareLockedUpgradable(() -> ClasspathPatcher.CL, Objects::isNull,
			ClasspathPatcher::initCl);
	}

	private static ClassLoader initCl(ClassLoader oldCl) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URLClassLoader ucl = Tools.cast(URLClassLoader.class, cl);
		ClasspathPatcher.ADD_URL = ClasspathPatcher.getConsumer(ucl);
		if (ClasspathPatcher.ADD_URL == null) {
			final CPCL newCl = new CPCL(ClasspathPatcher.NO_URLS, cl);
			ucl = newCl;
			Thread.currentThread().setContextClassLoader(newCl);
			ClasspathPatcher.ADD_URL = newCl::addURL;
		}
		ClasspathPatcher.CL = ucl;
		return ClasspathPatcher.CL;
	}

	private static Method findMethodRecursively(Class<?> c, String name, Class<?>... parameterTypes) {
		while (c != null) {
			try {
				return c.getDeclaredMethod(name, parameterTypes);
			} catch (NoSuchMethodException e) {
				// That's ok...the parent might succeed
			} catch (SecurityException e) {
				return null;
			}
			c = c.getSuperclass();
		}
		return null;
	}

	private static Consumer<URL> getConsumer(final URLClassLoader ucl) {
		if (ucl == null) { return null; }
		Class<? extends ClassLoader> clclass = ucl.getClass();
		try {
			Method method = ClasspathPatcher.findMethodRecursively(clclass, "addURL", URL.class);
			if (method == null) { return null; }

			try {
				method.setAccessible(true);
			} catch (SecurityException e) {
				// Ok ... we couldn't disable the checks... let's try calling it anyway
			}
			// If this invocation succeeds, we can safely return a consumer. If it fails,
			// then this method is not accessible to us...
			method.invoke(ucl, ClasspathPatcher.NULL_URL);
			final ClassLoader newCl = ucl;
			Consumer<URL> consumer = (url) -> {
				try {
					method.invoke(newCl, url);
				} catch (Exception e) {
					if (ClasspathPatcher.LOG.isDebugEnabled()) {
						ClasspathPatcher.LOG.error("Failed to add the URL [{}] to the classpath", url, e);
					}
				}
			};
			// This tells us if the method is reachable or not...
			consumer.accept(null);
			return consumer;
		} catch (Throwable t) {
			if (ClasspathPatcher.LOG.isDebugEnabled()) {
				ClasspathPatcher.LOG.warn("Failed to introspect the addURL() method from {}",
					clclass.getCanonicalName());
			}
			return null;
		}
	}

	public static final boolean discoverPatches() {
		return ClasspathPatcher.discoverPatches(false);
	}

	public static final boolean discoverPatches(boolean verbose) {
		try (MutexAutoLock lock = ClasspathPatcher.LOCK.autoMutexLock()) {
			Set<URL> patches = new LinkedHashSet<>();
			PluggableServiceLocator<ClasspathPatcher> patchers = new PluggableServiceLocator<>(ClasspathPatcher.class);
			patchers.setHideErrors(false);
			for (ClasspathPatcher p : patchers) {
				// Make sure we only include the patchers we're interested in
				Collection<URL> l = null;
				try {
					l = p.getPatches(verbose);
				} catch (Exception e) {
					if (verbose) {
						if (ClasspathPatcher.LOG.isDebugEnabled()) {
							ClasspathPatcher.LOG.warn("Failed to load the classpath patches from [{}]",
								p.getClass().getCanonicalName(), e);
						} else {
							ClasspathPatcher.LOG.warn("Failed to load the classpath patches from [{}]: {}",
								p.getClass().getCanonicalName(), e.getMessage());
						}
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
					if (verbose) {
						ClasspathPatcher.LOG.info("Classpath addition: [{}]", u);
					}
				} catch (IOException e) {
					if (verbose) {
						if (ClasspathPatcher.LOG.isDebugEnabled()) {
							ClasspathPatcher.LOG.error("Failed to add [{}] to the classpath", u, e);
						} else {
							ClasspathPatcher.LOG.error("Failed to add [{}] to the classpath: {}", u, e.getMessage());
						}
					}
				}
			}
			return ret;
		}
	}

	public static Set<String> getAdditions() {
		return ClasspathPatcher.LOCK.shareLocked(() -> Tools.freezeSet(new LinkedHashSet<>(ClasspathPatcher.ADDED)));
	}

	public static boolean addToClassPath(URL u) throws IOException {
		if (u == null) { throw new IllegalArgumentException("Must provide a URL to add to the classpath"); }
		Boolean ret = ClasspathPatcher.LOCK.shareLockedUpgradable(() -> !ClasspathPatcher.ADDED.contains(u.toString()),
			() -> {
				if (ClasspathPatcher.ADD_URL != null) {
					try {
						ClasspathPatcher.ADD_URL.accept(u);
						return ClasspathPatcher.ADDED.add(u.toString());
					} catch (Throwable t) {
						throw new IOException(String.format("Failed to add the URL [%s] to the classloader", u), t);
					}
				}
				return Boolean.FALSE;
			});
		return ((ret != null) && ret.booleanValue());
	}

	public static boolean addToClassPath(File f) throws IOException {
		if (f == null) { throw new IllegalArgumentException("Must provide a File to add to the classpath"); }
		return ClasspathPatcher.addToClassPath(f.toURI().toURL());
	}

	public static boolean addToClassPath(String f) throws IOException {
		if (f == null) { throw new IllegalArgumentException("Must provide a File path to add to the classpath"); }
		return ClasspathPatcher.addToClassPath(new File(f));
	}

	protected ClasspathPatcher() {
	}

	public abstract Collection<URL> getPatches(boolean verbose) throws Exception;

	public final Collection<URL> applyPatches(boolean verbose) throws Exception {
		Collection<URL> patches = getPatches(verbose);
		Collection<URL> applied = new ArrayList<>();
		if (patches != null) {
			for (URL u : patches) {
				if (u == null) {
					continue;
				}
				ClasspathPatcher.addToClassPath(u);
				applied.add(u);
			}
		}
		return applied;
	}
}