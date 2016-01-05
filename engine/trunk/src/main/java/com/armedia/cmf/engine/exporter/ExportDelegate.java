package com.armedia.cmf.engine.exporter;

import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferDelegate;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;

public abstract class ExportDelegate<T, S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V, ?>, DF extends ExportDelegateFactory<S, W, V, C, E>, E extends ExportEngine<S, W, V, C, ?, DF>>
	extends TransferDelegate<T, S, V, C, DF, E> {
	protected final T object;
	protected final ExportTarget exportTarget;
	protected final String label;
	protected final String batchId;

	protected ExportDelegate(DF factory, Class<T> objectClass, T object) throws Exception {
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

	protected abstract CmfType calculateType(T object) throws Exception;

	public final CmfType getType() {
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

	protected abstract Collection<? extends ExportDelegate<?, S, W, V, C, DF, ?>> identifyRequirements(
		CmfObject<V> marshalled, C ctx) throws Exception;

	final CmfObject<V> marshal(C ctx, ExportTarget referrent) throws ExportException {
		CmfType type = getType();

		CmfObject<V> marshaled = new CmfObject<V>(this.factory.getTranslator(), type, getObjectId(), getSearchKey(),
			getBatchId(), getLabel(), type.name(), ctx.getProductName(), ctx.getProductVersion(), null);
		if (!marshal(ctx, marshaled)) { return null; }
		this.factory.getEngine().setReferrent(marshaled, referrent);
		return marshaled;
	}

	protected abstract boolean marshal(C ctx, CmfObject<V> object) throws ExportException;

	protected abstract Collection<? extends ExportDelegate<?, S, W, V, C, DF, ?>> identifyDependents(
		CmfObject<V> marshalled, C ctx) throws Exception;

	protected abstract List<CmfContentInfo> storeContent(C ctx, CmfAttributeTranslator<V> translator,
		CmfObject<V> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore) throws Exception;
}