package com.armedia.caliente.cli.caliente.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.utils.ProcessFuture;

public class DFCLauncher extends AbstractLauncher {

	public static void main(String[] args) throws Throwable {
		if (!CLIParam.parse(args)) { return; }

		Map<CLIParam, List<String>> cliParams = CLIParam.getParsed();
		if (cliParams == null) { return; }

		Collection<File> classpath = new ArrayList<File>();
		Map<String, String> environment = new HashMap<String, String>();

		String var = null;
		File base = null;
		File tgt = null;

		// First, add the ${PWD}/cfg directory to the classpath - whether it exists or not
		var = System.getProperty("user.dir");
		base = new File(var);
		classpath.add(base);
		tgt = new File(var, "cfg");
		classpath.add(tgt);

		boolean dctm = true;

		if (dctm) {
			// Next, add ${DOCUMENTUM}/config to the classpath, if the server is a DCTM server
			var = System.getenv(AbstractLauncher.ENV_DOCUMENTUM);
			if (cliParams.containsKey(CLIParam.dctm)) {
				// DFC is specified
				List<String> l = cliParams.get(CLIParam.dctm);
				var = (!l.isEmpty() ? l.get(0) : null);
			} else {
				// Go with the environment
				if (var == null) { throw new RuntimeException(
					String.format("The environment variable [%s] is not set", AbstractLauncher.ENV_DOCUMENTUM)); }
			}

			base = new File(var).getCanonicalFile();
			if (!base.isDirectory()) { throw new FileNotFoundException(
				String.format("Could not find the directory [%s]", base.getAbsolutePath())); }

			// Make sure the environment reflects our changes
			environment.put(AbstractLauncher.ENV_DOCUMENTUM, base.getCanonicalPath());

			tgt = new File(base, "config");
			if (!base.isDirectory()) { throw new FileNotFoundException(
				String.format("Could not find the directory [%s]", tgt.getAbsolutePath())); }

			classpath.add(tgt);

			// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
			var = System.getenv(AbstractLauncher.ENV_DOCUMENTUM_SHARED);
			if (cliParams.containsKey(CLIParam.dfc)) {
				// DFC is specified
				List<String> l = cliParams.get(CLIParam.dfc);
				var = (!l.isEmpty() ? l.get(0) : null);
			} else {
				// Go with the environment
				if (var == null) { throw new RuntimeException(String.format("The environment variable [%s] is not set",
					AbstractLauncher.ENV_DOCUMENTUM_SHARED)); }
			}

			// Next, is it a directory?
			base = new File(var).getCanonicalFile();
			if (!base.isDirectory()) { throw new FileNotFoundException(
				String.format("Could not find the directory [%s]", base.getAbsolutePath())); }

			// Make sure the environment reflects our changes
			environment.put(AbstractLauncher.ENV_DOCUMENTUM_SHARED, base.getCanonicalPath());

			// Next, does dctm.jar exist in there?
			tgt = new File(base, AbstractLauncher.DCTM_JAR);
			if (!tgt.isFile()) { throw new FileNotFoundException(
				String.format("Could not find the JAR file [%s]", tgt.getAbsolutePath())); }

			// Next, to the classpath
			classpath.add(tgt);
		}

		// Finally, launch the main class
		// We launch like this because we have to patch the classpath before we link into the rest
		// of the code. If we don't do it like this, the app will refuse to launch altogether
		ProcessFuture.execClass(CMSMFLauncher.class, classpath, environment, args);
	}
}