package com.armedia.cmf.engine;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.StoredObjectType;

public abstract class TransferDelegate<T, S, V, E extends TransferEngine<S, V, ?, ?, ?>> {
	protected final E engine;
	protected final Class<T> objectClass;
	protected final T object;
	protected final ExportTarget exportTarget;

	protected TransferDelegate(E engine, Class<T> objectClass, T object) {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to process with"); }
		if (objectClass == null) { throw new IllegalArgumentException("Must provide an object class to work with"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object to process"); }
		this.engine = engine;
		this.objectClass = objectClass;
		this.object = object;
		this.exportTarget = new ExportTarget(getType(), getObjectId(), getSearchKey());
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

	public abstract StoredObjectType getType();

	public abstract String getLabel();

	public abstract String getObjectId();

	public abstract String getSearchKey();

	public abstract String getBatchId();
}