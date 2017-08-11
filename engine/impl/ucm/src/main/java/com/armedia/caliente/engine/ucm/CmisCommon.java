package com.armedia.caliente.engine.ucm;

import java.util.Collections;
import java.util.Set;

public interface CmisCommon {
	public static final String TARGET_NAME = "cmis";

	public static final Set<String> TARGETS = Collections.singleton(CmisCommon.TARGET_NAME);
}