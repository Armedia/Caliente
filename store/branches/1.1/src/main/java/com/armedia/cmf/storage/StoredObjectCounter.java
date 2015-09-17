package com.armedia.cmf.storage;

import com.armedia.commons.utilities.EnumeratedCounter;

public final class StoredObjectCounter<R extends Enum<R>> extends EnumeratedCounter<StoredObjectType, R> {

	public StoredObjectCounter(Class<R> rClass) {
		super(StoredObjectType.class, rClass);
	}
}