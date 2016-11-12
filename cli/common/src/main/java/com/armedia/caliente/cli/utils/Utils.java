package com.armedia.caliente.cli.utils;

import java.io.File;
import java.io.IOException;

class Utils {
	public static File newFileObject(String path) {
		return Utils.newFileObject(null, path);
	}

	public static File newFileObject(File parent, String path) {
		File f = (parent != null ? new File(parent, path) : new File(path));
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// this.log.warn(String.format("Failed to canonicalize the path for [%s]",
			// f.getAbsolutePath()), e);
			// Do nothing, for now
		} finally {
			f = f.getAbsoluteFile();
		}
		return f;
	}
}