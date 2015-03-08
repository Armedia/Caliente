package com.delta.cmsmf.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.delta.cmsmf.cfg.CLIParam;

public class DctmClasspathPatcher extends ClasspathPatcher {

	protected static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	protected static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	protected static final String DCTM_JAR = "dctm.jar";

	public DctmClasspathPatcher() {
		super("dctm");
	}

	@Override
	public List<URL> getPatches(String engine) {
		List<URL> ret = new ArrayList<URL>(3);
		try {

			String var = System.getProperty("user.dir");
			File f = new File(var);
			ret.add(f.toURI().toURL());

			// Next, add ${DOCUMENTUM}/config to the classpath
			var = CLIParam.dctm.getString(System.getenv(DctmClasspathPatcher.ENV_DOCUMENTUM));
			// Go with the environment
			if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
				DctmClasspathPatcher.ENV_DOCUMENTUM)); }

			f = new File(var).getCanonicalFile();
			if (!f.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
				f.getAbsolutePath())); }
			ret.add(new File(f, "config").toURI().toURL());

			// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
			var = CLIParam.dfc.getString(System.getenv(DctmClasspathPatcher.ENV_DOCUMENTUM_SHARED));
			// Go with the environment
			if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
				DctmClasspathPatcher.ENV_DOCUMENTUM_SHARED)); }

			// Next, is it a directory?
			f = new File(var).getCanonicalFile();
			if (!f.isDirectory()) { throw new FileNotFoundException(String.format(
				"Could not find the [%s] directory [%s]", DctmClasspathPatcher.ENV_DOCUMENTUM_SHARED,
				f.getAbsolutePath())); }

			// Next, does dctm.jar exist in there?
			File tgt = new File(f, DctmClasspathPatcher.DCTM_JAR);
			if (!tgt.isFile()) { throw new FileNotFoundException(String.format("Could not find the JAR file [%s]",
				tgt.getAbsolutePath())); }

			// Next, to the classpath
			ret.add(tgt.toURI().toURL());
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to configure the dynamic classpath for engine [%s]",
				engine), e);
		}
		return ret;
	}
}