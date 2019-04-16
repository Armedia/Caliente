package com.armedia.caliente.engine.local.common;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public final class LocalCommon {
	public static final String TARGET_NAME = "local";
	public static final Set<String> TARGETS = Collections.singleton(LocalCommon.TARGET_NAME);

	private LocalCommon() {
	}

	public static File getRootDirectory(CfgTools cfg) {
		String root = cfg.getString(LocalSetting.ROOT);
		if (root == null) { return null; }
		return Tools.canonicalize(new File(root));
	}

	public static String calculateId(String portablePath) {
		if ((portablePath == null) || Tools.equals("/", portablePath)) { return null; }
		return DigestUtils.sha256Hex(portablePath);
	}

	public static String getPortablePath(String path) {
		if (StringUtils.isEmpty(path)) { return null; }
		return FileNameTools.reconstitute(FileNameTools.tokenize(path), true, false, '/');
	}
}