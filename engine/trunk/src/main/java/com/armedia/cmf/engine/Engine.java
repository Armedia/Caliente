package com.armedia.cmf.engine;

import com.armedia.cmf.storage.ObjectStorageTranslator;

public interface Engine<S, T, V> {
	public ObjectStorageTranslator<T, V> getTranslator();

	public SessionFactory<S> getSessionFactory();
}