package com.armedia.cmf.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.StoredObjectType;

public abstract class TransferDelegate<T, S, V, E extends TransferEngine<S, V, ?, ?, ?>> {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final E engine;
	protected final Class<T> objectClass;
	protected final T object;
	protected final ExportTarget exportTarget;
	protected final StoredObjectType type;
	protected final String label;
	protected final String batchId;

	protected TransferDelegate(E engine, Class<T> objectClass, T object) throws Exception {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to process with"); }
		if (objectClass == null) { throw new IllegalArgumentException("Must provide an object class to work with"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object to process"); }
		this.engine = engine;
		this.objectClass = objectClass;
		this.object = object;
		this.exportTarget = new ExportTarget(getType(), calculateObjectId(object), calculateSearchKey(object));
		this.type = calculateType(object);
		this.label = calculateLabel(object);
		this.batchId = calculateBatchId(object);
	}

	protected final T castObject(Object o) {
		// This should NEVER fail...but if it does, it's well-deserved and we make no effort to
		// catch it or soften the blow
		return this.objectClass.cast(o);
	}

	public final E getEngine() {
		return this.engine;
	}

	public final T getObject() {
		return this.object;
	}

	public final ExportTarget getExportTarget() {
		return this.exportTarget;
	}

	protected abstract StoredObjectType calculateType(T object) throws Exception;

	public final StoredObjectType getType() {
		return this.type;
	}

	protected abstract String calculateLabel(T object) throws Exception;

	public final String getLabel() {
		return this.label;
	}

	protected abstract String calculateObjectId(T object) throws Exception;

	public final String getObjectId() {
		return this.exportTarget.getId();
	}

	protected abstract String calculateSearchKey(T object) throws Exception;

	public final String getSearchKey() {
		return this.exportTarget.getSearchKey();
	}

	protected String calculateBatchId(T object) throws Exception {
		return null;
	}

	public final String getBatchId() {
		return this.batchId;
	}
}