package com.armedia.caliente.engine;

import com.armedia.caliente.store.CmfObjectRef;

public interface WarningTracker {

	public void trackWarning(CmfObjectRef ref, String format, Object... args);

}