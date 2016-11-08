package com.armedia.caliente.cli.caliente.launcher;

import com.armedia.caliente.cli.caliente.exception.CMSMFException;
import com.armedia.caliente.store.CmfObjectStore;

public interface CMSMFMain {

	public void run() throws CMSMFException;

	public CmfObjectStore<?, ?> getObjectStore();
}