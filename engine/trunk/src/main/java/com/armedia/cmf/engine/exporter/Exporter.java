package com.armedia.cmf.engine.exporter;

import java.util.Collection;
import java.util.Map;

import com.armedia.cmf.engine.Engine;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

public interface Exporter<S, T, V> extends Engine<S, T, V> {

	public Iterable<ExportTarget> findExportResults(S session, Map<String, Object> settings) throws Exception;

	public T getObject(S session, StoredObjectType type, String id) throws Exception;

	public Collection<T> identifyRequirements(S session, T object) throws Exception;

	public Collection<T> identifyDependents(S session, T object) throws Exception;

	public StoredObject<V> marshal(S session, T object) throws ExportException;

	public void storeContent(S session, T object, ContentStreamStore streamStore) throws Exception;

}