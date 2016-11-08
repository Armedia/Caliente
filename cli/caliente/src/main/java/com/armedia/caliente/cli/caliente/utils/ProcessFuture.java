/**
 *
 */

package com.armedia.caliente.cli.caliente.utils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.SystemUtils;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;.rivera@armedia.com
 *
 */
public class ProcessFuture {

	private final Process process;
	private boolean aborted = false;

	public ProcessFuture(Process process) {
		if (process == null) { throw new IllegalArgumentException("Must provide a non-null process"); }
		this.process = process;
	}

	private final Runnable waiter = new Runnable() {
		@Override
		public void run() {
			try {
				ProcessFuture.this.process.waitFor();
			} catch (InterruptedException e) {
				// Do nothing, but restore interrupted status
				Thread.currentThread().interrupt();
			}
		}
	};

	public synchronized boolean abort() {
		if (!isDone()) {
			this.aborted = true;
			this.process.destroy();
		}
		return this.aborted;
	}

	public synchronized boolean isAborted() {
		return this.aborted;
	}

	private Integer getExitStatus() {
		try {
			return this.process.exitValue();
		} catch (IllegalThreadStateException e) {
			return null;
		}
	}

	public boolean isDone() {
		return (getExitStatus() != null);
	}

	public Integer get() throws InterruptedException {
		return this.process.waitFor();
	}

	public Integer get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {

		final long waitInMillis = unit.convert(timeout, TimeUnit.MILLISECONDS);
		if (waitInMillis > 0) {
			final Thread t = new Thread(this.waiter);
			t.setDaemon(true);
			t.start();
			try {
				t.join(waitInMillis);
			} catch (final InterruptedException e) {
				// If we're interrupted, we interrupt the waiter thread as well
				t.interrupt();
				throw e;
			}
		}

		final Integer ret = getExitStatus();
		if (ret == null) { throw new TimeoutException("Timed out waiting for the child process to exit"); }
		return ret;
	}

	public static ProcessFuture execClass(Class<?> klass, Collection<File> classpath, Map<String, String> environment,
		String... args) throws IOException, InterruptedException {

		try {
			Method m = klass.getMethod("main", String[].class);
			if (!Modifier.isStatic(m.getModifiers())) { throw new IllegalArgumentException(String.format(
				"Class [%s] lacks a visible static main() method%n", klass.getCanonicalName())); }
		} catch (SecurityException e) {
			// Can't tell, so we keep going...
		} catch (NoSuchMethodException e) {
			// No such method...
			throw new IllegalArgumentException(String.format("Class [%s] lacks a main() method%n",
				klass.getCanonicalName()), e);
		}

		// This will help identify
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmArgs = runtimeMxBean.getInputArguments();

		String javaHome = System.getProperty("java.home");
		File javaBin = new File(javaHome);
		javaBin = new File(javaBin, "bin");
		javaBin = new File(javaBin, (SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java"));

		StringBuilder b = new StringBuilder();

		// First, parse out the current classpath
		String currentCp = System.getProperty("java.class.path");
		StringTokenizer tok = new StringTokenizer(currentCp, File.pathSeparator);
		while (tok.hasMoreElements()) {
			final String token = tok.nextToken();
			if (token.length() == 0) {
				continue;
			}
			if (b.length() > 0) {
				b.append(File.pathSeparatorChar);
			}
			b.append(token);
		}

		// Current classpath has been pre-pended, we add the additional items
		for (File f : classpath) {
			if (b.length() > 0) {
				b.append(File.pathSeparatorChar);
			}
			b.append(f.getAbsolutePath());
		}
		List<String> command = new ArrayList<String>();
		command.add(javaBin.getAbsolutePath());

		// Add the classpath first
		if (b.length() > 0) {
			command.add("-cp");
			command.add(b.toString());
		}

		for (String s : jvmArgs) {
			// Ignore -cp and -classpath
			if ((s == null) || "-classpath".equals(s) || "-cp".equals(s)) {
				continue;
			}
			command.add(s);
		}

		// Add the main class
		command.add(klass.getCanonicalName());

		// Add the class arguments
		for (String s : args) {
			if (s == null) {
				continue;
			}
			command.add(s);
		}

		// Build the process
		final ProcessBuilder builder = new ProcessBuilder(command);
		if ((environment != null) && !environment.isEmpty()) {
			builder.environment().putAll(environment);
		}

		return new ProcessFuture(builder.start());
	}
}