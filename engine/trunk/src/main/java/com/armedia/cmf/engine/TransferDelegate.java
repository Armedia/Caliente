package com.armedia.cmf.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.StoredObjectType;

public abstract class TransferDelegate<T, S, V, C extends TransferContext<S, V>, F extends TransferDelegateFactory<S, V, C, E>, E extends TransferEngine<S, V, C, ?, ?, ?>> {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final F factory;
	protected final Class<T> objectClass;
	protected final T object;
	protected final ExportTarget exportTarget;
	protected final String label;
	protected final String batchId;

	protected TransferDelegate(F factory, Class<T> objectClass, T object) throws Exception {
		if (factory == null) { throw new IllegalArgumentException("Must provide a factory to process with"); }
		if (objectClass == null) { throw new IllegalArgumentException("Must provide an object class to work with"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object to process"); }
		this.factory = factory;
		this.objectClass = objectClass;
		this.object = object;

		// Now we invoke everything that needs to be calculated
		this.exportTarget = new ExportTarget(calculateType(object), calculateObjectId(object),
			calculateSearchKey(object));
		this.label = calculateLabel(object);
		this.batchId = calculateBatchId(object);
	}

	protected final T castObject(Object o) {
		// This should NEVER fail...but if it does, it's well-deserved and we make no effort to
		// catch it or soften the blow
		return this.objectClass.cast(o);
	}

	public final T getObject() {
		return this.object;
	}

	public final ExportTarget getExportTarget() {
		return this.exportTarget;
	}

	protected abstract StoredObjectType calculateType(T object) throws Exception;

	public final StoredObjectType getType() {
		return this.exportTarget.getType();
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