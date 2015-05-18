package com.armedia.cmf.storage;

import com.armedia.commons.utilities.EnumeratedCounter;

public final class CmfObjectCounter<R extends Enum<R>> extends EnumeratedCounter<CmfType, R> {

	public CmfObjectCounter(Class<R> rClass) {
		super(CmfType.class, rClass);
	}
}