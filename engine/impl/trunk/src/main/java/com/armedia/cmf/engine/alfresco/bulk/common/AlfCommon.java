package com.armedia.cmf.engine.alfresco.bulk.common;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.armedia.commons.utilities.CfgTools;

public final class AlfCommon {
	public static final String TARGET_NAME = "alfresco";
	public static final Set<String> TARGETS = Collections.singleton(AlfCommon.TARGET_NAME);

	public static final String ROOT = "root";

	private AlfCommon() {

	}

	public static File getRootDirectory(CfgTools cfg) throws IOException {
		String root = cfg.getString(AlfCommon.ROOT);
		if (root == null) { return null; }
		return new File(root).getCanonicalFile();
	}
}