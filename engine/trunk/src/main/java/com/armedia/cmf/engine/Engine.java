package com.armedia.cmf.engine;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.commons.utilities.CfgTools;

public interface Engine<S, T, V> {
	public void init(CfgTools config) throws Exception;

	public ObjectStorageTranslator<T, V> getTranslator();

	public SessionFactory<S> getSessionFactory();

	public void close();
}