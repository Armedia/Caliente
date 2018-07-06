package com.armedia.caliente.cli.caliente.launcher.dctm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;

public class DctmClasspathPatcher implements LaunchClasspathHelper {

	protected static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	protected static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	protected static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	protected static final String DCTM_JAR = "dctm.jar";
	protected static final String DFC_TEST_CLASS = "com.documentum.fc.client.IDfFolder";

	private final Logger log = LoggerFactory.getLogger(getClass());

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
	public Collection<URL> getClasspathPatchesPre(OptionValues values) {
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

		List<URL> ret = new ArrayList<>(3);
		try {
			String var = values.getString(CLIParam.dfc_prop, "dfc.properties");
			if (var != null) {
				File f = createFile(var);
				if (f.exists() && f.isFile() && f.canRead()) {
					System.setProperty(DctmClasspathPatcher.DFC_PROPERTIES_PROP, f.getAbsolutePath());
				}
			}

			// Next, add ${DOCUMENTUM}/config to the classpath
			var = values.getString(CLIParam.dctm, System.getenv(DctmClasspathPatcher.ENV_DOCUMENTUM));
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
			var = values.getString(CLIParam.dfc, System.getenv(DctmClasspathPatcher.ENV_DOCUMENTUM_SHARED));
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
			throw new RuntimeException("Failed to configure the dynamic classpath for Documentum", e);
		}
		return ret;
	}

	@Override
	public Collection<URL> getClasspathPatchesPost(OptionValues values) {
		return Collections.emptyList();
	}
}