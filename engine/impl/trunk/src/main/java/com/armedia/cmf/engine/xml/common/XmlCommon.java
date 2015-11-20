package com.armedia.cmf.engine.xml.common;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.armedia.commons.utilities.CfgTools;

public final class XmlCommon {
	public static final String TARGET_NAME = "xml";
	public static final Set<String> TARGETS = Collections.singleton(XmlCommon.TARGET_NAME);

	public static final String ROOT = "root";

	private XmlCommon() {

	}

	public static File getRootDirectory(CfgTools cfg) throws IOException {
		String root = cfg.getString(XmlCommon.ROOT);
		if (root == null) { return null; }
		return new File(root).getCanonicalFile();
	}
}