package com.armedia.cmf.engine.exporter;

import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferDelegate;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.AttributeTranslator;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

public abstract class ExportDelegate<T, S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V>, F extends ExportDelegateFactory<S, W, V, C, E>, E extends ExportEngine<S, W, V, C, F>>
	extends TransferDelegate<T, S, V, C, F, E> {
	protected final T object;
	protected final ExportTarget exportTarget;
	protected final String label;
	protected final String batchId;

	protected ExportDelegate(F factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass);
		if (object == null) { throw new IllegalArgumentException("Must provide a source object to export"); }
		this.object = object;

		// Now we invoke everything that needs to be calculated
		this.exportTarget = new ExportTarget(calculateType(object), calculateObjectId(object),
			calculateSearchKey(object));
		this.label = calculateLabel(object);
		this.batchId = calculateBatchId(object);
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

	protected abstract Collection<? extends ExportDelegate<?, S, W, V, C, F, ?>> identifyRequirements(
		StoredObject<V> marshalled, C ctx) throws Exception;

	final StoredObject<V> marshal(C ctx, ExportTarget referrent) throws ExportException {
		StoredObjectType type = getType();
		StoredObject<V> marshaled = new StoredObject<V>(type, getObjectId(), getSearchKey(), getBatchId(), getLabel(),
			type.name());
		if (!marshal(ctx, marshaled)) { return null; }
		this.factory.getEngine().setReferrent(marshaled, referrent);
		return marshaled;
	}

	protected abstract boolean marshal(C ctx, StoredObject<V> object) throws ExportException;

	protected abstract Collection<? extends ExportDelegate<?, S, W, V, C, F, ?>> identifyDependents(
		StoredObject<V> marshalled, C ctx) throws Exception;

	protected abstract List<ContentInfo> storeContent(S session, AttributeTranslator<V> translator,
		StoredObject<V> marshalled, ExportTarget referrent, ContentStore<?> streamStore) throws Exception;
}