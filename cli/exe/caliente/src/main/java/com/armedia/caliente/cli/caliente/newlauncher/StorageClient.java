package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;

import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;

public interface StorageClient {

	public CmfObjectStore<?, ?> getObjectStore();

	public CmfContentStore<?, ?, ?> getContentStore();

	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers();

}