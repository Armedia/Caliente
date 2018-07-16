package com.armedia.caliente.cli.caliente.cfg;

import java.io.File;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;

public class CalienteState {

	private final File objectStoreLocation;
	private final CmfObjectStore<?, ?> objectStore;

	private final File contentStoreLocation;
	private final CmfContentStore<?, ?, ?> contentStore;

	public CalienteState(File objectStoreLocation, CmfObjectStore<?, ?> objectStore, File contentStoreLocation,
		CmfContentStore<?, ?, ?> contentStore) {
		this.objectStoreLocation = objectStoreLocation;
		this.objectStore = objectStore;
		this.contentStoreLocation = contentStoreLocation;
		this.contentStore = contentStore;
	}

	public File getObjectStoreLocation() {
		return this.objectStoreLocation;
	}

	public CmfObjectStore<?, ?> getObjectStore() {
		return this.objectStore;
	}

	public File getContentStoreLocation() {
		return this.contentStoreLocation;
	}

	public CmfContentStore<?, ?, ?> getContentStore() {
		return this.contentStore;
	}
}