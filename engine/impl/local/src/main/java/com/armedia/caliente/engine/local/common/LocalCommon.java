package com.armedia.caliente.engine.local.common;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.armedia.commons.utilities.CfgTools;

public final class LocalCommon {
	public static final String TARGET_NAME = "local";
	public static final Set<String> TARGETS = Collections.singleton(LocalCommon.TARGET_NAME);

	private LocalCommon() {

	}

	public static File getRootDirectory(CfgTools cfg) throws IOException {
		String root = cfg.getString(LocalSetting.ROOT);
		if (root == null) { return null; }
		return new File(root).getCanonicalFile();
	}
}