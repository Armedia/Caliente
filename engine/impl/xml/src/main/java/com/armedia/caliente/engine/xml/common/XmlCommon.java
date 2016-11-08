package com.armedia.caliente.engine.xml.common;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.armedia.commons.utilities.CfgTools;

public final class XmlCommon {
	public static final String TARGET_NAME = "xml";
	public static final Set<String> TARGETS = Collections.singleton(XmlCommon.TARGET_NAME);

	private XmlCommon() {

	}

	public static File getRootDirectory(CfgTools cfg) throws IOException {
		String root = cfg.getString(XmlSetting.ROOT);
		if (root == null) { return null; }
		return new File(root).getCanonicalFile();
	}
}