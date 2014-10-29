package com.armedia.cmf.engine.exporter;

import com.armedia.cmf.storage.DependencyManager;
import com.armedia.cmf.storage.StoredObject;

public interface ObjectExporter<T, S, V> {

	public StoredObject<?> exportObject(T object) throws ExportException;

	public StoredObject<?> exportObjectById(String id) throws ExportException;

	public void persistRequirements(T object, ExportContext<S, V> ctx, DependencyManager<T, V> dependencyManager)
		throws ExportException;

	public void persistDependents(T object, ExportContext<S, V> ctx, DependencyManager<T, V> dependencyManager)
		throws ExportException;
}