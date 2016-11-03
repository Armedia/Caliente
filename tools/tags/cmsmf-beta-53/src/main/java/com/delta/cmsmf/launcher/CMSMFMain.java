package com.delta.cmsmf.launcher;

import com.armedia.cmf.storage.CmfObjectStore;
import com.delta.cmsmf.exception.CMSMFException;

public interface CMSMFMain {

	public void run() throws CMSMFException;

	public CmfObjectStore<?, ?> getObjectStore();
}