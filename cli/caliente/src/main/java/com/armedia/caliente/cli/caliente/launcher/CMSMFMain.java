package com.armedia.caliente.cli.caliente.launcher;

import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.store.CmfObjectStore;

public interface CMSMFMain {

	public void run() throws CalienteException;

	public CmfObjectStore<?, ?> getObjectStore();
}