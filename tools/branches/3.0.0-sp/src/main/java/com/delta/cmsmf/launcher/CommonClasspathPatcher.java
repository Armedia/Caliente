package com.delta.cmsmf.launcher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.delta.cmsmf.utils.ClasspathPatcher;

public class CommonClasspathPatcher extends ClasspathPatcher {

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
			String var = null;
			File base = null;
			File tgt = null;

			// First, add the ${PWD}/cfg directory to the classpath - whether it exists or not
			var = System.getProperty("user.dir");
			base = new File(var);
			tgt = new File(var, "cfg");
			ret.add(base.toURI().toURL());
			ret.add(tgt.toURI().toURL());
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to configure the dynamic classpath for engine [%s]",
				engine), e);
		}
		return ret;
	}
}