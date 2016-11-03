package com.delta.cmsmf.launcher.dctm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.utils.ClasspathPatcher;

public class DctmClasspathPatcher extends ClasspathPatcher {

	protected static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	protected static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	protected static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	protected static final String DCTM_JAR = "dctm.jar";
	protected static final String DFC_TEST_CLASS = "com.documentum.fc.client.IDfFolder";

	private final Logger log = LoggerFactory.getLogger(getClass());

	public DctmClasspathPatcher() {
		super("dctm");
	}

	private File createFile(String path) {
		return createFile(null, path);
	}

	private File createFile(File parent, String path) {
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
	public List<URL> getPatches(String engine) {
		final boolean dfcFound;
		{
			boolean dfc = false;
			try {
				Class.forName(DctmClasspathPatcher.DFC_TEST_CLASS);
				dfc = true;
			} catch (Exception e) {
				dfc = false;
			}
			dfcFound = dfc;
		}

		List<URL> ret = new ArrayList<URL>(3);
		try {
			String var = CLIParam.dfc_prop.getString("dfc.properties");
			if (var != null) {
				File f = createFile(var);
				if (f.exists() && f.isFile() && f.canRead()) {
					System.setProperty(DctmClasspathPatcher.DFC_PROPERTIES_PROP, f.getAbsolutePath());
				}
			}

			// Next, add ${DOCUMENTUM}/config to the classpath
			var = CLIParam.dctm.getString(System.getenv(DctmClasspathPatcher.ENV_DOCUMENTUM));
			// Go with the environment
			if (var == null) {
				String msg = String.format("The environment variable [%s] is not set",
					DctmClasspathPatcher.ENV_DOCUMENTUM);
				if (!dfcFound) { throw new RuntimeException(msg); }
				this.log.warn("{}, integrated DFC may encounter errors", msg);
			}

			if (var != null) {
				File f = createFile(var);
				if (!f.exists()) {
					FileUtils.forceMkdir(f);
				}
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the directory [%s]", f.getAbsolutePath())); }

				ret.add(createFile(f, "config").toURI().toURL());
			}

			// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
			var = CLIParam.dfc.getString(System.getenv(DctmClasspathPatcher.ENV_DOCUMENTUM_SHARED));
			// Go with the environment
			if (var == null) {
				String msg = String.format("The environment variable [%s] is not set",
					DctmClasspathPatcher.ENV_DOCUMENTUM_SHARED);
				if (!dfcFound) { throw new RuntimeException(msg); }
				this.log.warn("{}, integrated DFC may encounter errors", msg);
			}

			if (var != null) {
				// Next, is it a directory?
				File f = createFile(var);
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the [%s] directory [%s]", DctmClasspathPatcher.ENV_DOCUMENTUM_SHARED,
						f.getAbsolutePath())); }

				// Next, does dctm.jar exist in there?
				if (!dfcFound) {
					File tgt = createFile(f, DctmClasspathPatcher.DCTM_JAR);
					if (!tgt.isFile()) { throw new FileNotFoundException(
						String.format("Could not find the JAR file [%s]", tgt.getAbsolutePath())); }

					// Next, to the classpath
					ret.add(tgt.toURI().toURL());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(
				String.format("Failed to configure the dynamic classpath for engine [%s]", engine), e);
		}
		return ret;
	}
}