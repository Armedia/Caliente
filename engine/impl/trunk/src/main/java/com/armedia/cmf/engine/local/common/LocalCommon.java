package com.armedia.cmf.engine.local.common;

import java.util.Collections;
import java.util.Set;

public final class LocalCommon {
	public static final String TARGET_NAME = "local";
	public static final Set<String> TARGETS = Collections.singleton(LocalCommon.TARGET_NAME);

	private LocalCommon() {

	}
}