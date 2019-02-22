package com.armedia.caliente.cli.caliente.launcher;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;

public interface StorageClient {

	public CmfObjectStore<?> getObjectStore();

	public CmfContentStore<?, ?> getContentStore();

}