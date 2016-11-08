package com.armedia.caliente.cli.caliente.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.utils.ClasspathPatcher;

abstract class AbstractLauncher {
	protected static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	protected static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	protected static final String DCTM_JAR = "dctm.jar";

	protected static List<File> generateClasspath() throws IOException {
		List<File> ret = new ArrayList<File>();
		String var = null;
		File base = null;
		File tgt = null;

		// First, add the ${PWD}/cfg directory to the classpath - whether it exists or not
		var = System.getProperty("user.dir");
		base = new File(var);
		tgt = new File(var, "cfg");
		ret.add(base);
		ret.add(tgt);

		// Next, add ${DOCUMENTUM}/config to the classpath
		var = CLIParam.dctm.getString(System.getenv(AbstractLauncher.ENV_DOCUMENTUM));
		// Go with the environment
		if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
			AbstractLauncher.ENV_DOCUMENTUM)); }

		base = new File(var).getCanonicalFile();
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			base.getAbsolutePath())); }

		tgt = new File(base, "config");
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			tgt.getAbsolutePath())); }

		ret.add(tgt);

		// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
		var = CLIParam.dfc.getString(System.getenv(AbstractLauncher.ENV_DOCUMENTUM_SHARED));
		// Go with the environment
		if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
			AbstractLauncher.ENV_DOCUMENTUM_SHARED)); }

		// Next, is it a directory?
		base = new File(var).getCanonicalFile();
		if (!base.isDirectory()) { throw new FileNotFoundException(String.format("Could not find the directory [%s]",
			base.getAbsolutePath())); }

		// Next, does dctm.jar exist in there?
		tgt = new File(base, AbstractLauncher.DCTM_JAR);
		if (!tgt.isFile()) { throw new FileNotFoundException(String.format("Could not find the JAR file [%s]",
			tgt.getAbsolutePath())); }

		// Next, to the classpath
		ret.add(tgt);
		return ret;
	}

	protected static void patchClasspath() throws IOException {
		for (File f : AbstractLauncher.generateClasspath()) {
			ClasspathPatcher.addToClassPath(f);
		}
	}
}