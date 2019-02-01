package com.armedia.caliente.cli.utils;

import java.io.File;
import java.io.IOException;

public class CliUtils {

	static File newFileObject(String path) {
		return CliUtils.newFileObject(null, path);
	}

	static File newFileObject(File parent, String path) {
		File f = (parent != null ? new File(parent, path) : new File(path));
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// this.log.warn("Failed to canonicalize the path for [{}]",
			// f.getAbsolutePath(), e);
			// Do nothing, for now
		} finally {
			f = f.getAbsoluteFile();
		}
		return f;
	}
}