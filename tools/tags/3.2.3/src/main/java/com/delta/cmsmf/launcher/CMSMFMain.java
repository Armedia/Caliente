package com.delta.cmsmf.launcher;

import com.armedia.cmf.storage.ObjectStore;
import com.delta.cmsmf.exception.CMSMFException;

public interface CMSMFMain {

	public void run() throws CMSMFException;

	public boolean requiresDataStore();

	public ObjectStore<?, ?> getObjectStore();

	public boolean requiresCleanData();
}