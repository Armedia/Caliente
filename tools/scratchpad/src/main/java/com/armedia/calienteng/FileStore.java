package com.armedia.calienteng;

import java.util.Collection;

public abstract class FileStore {

	public abstract String getId();

	protected abstract boolean hasChanged(FileSpec fileSpec);

	protected abstract Collection<FileSpec> getChangesSince(long date);

	public abstract boolean isSynchronous();

}