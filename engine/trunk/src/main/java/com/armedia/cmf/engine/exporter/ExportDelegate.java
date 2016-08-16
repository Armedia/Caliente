package com.armedia.cmf.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferDelegate;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectRef;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.Tools;

public abstract class ExportDelegate<T, S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V, ?>, DF extends ExportDelegateFactory<S, W, V, C, E>, E extends ExportEngine<S, W, V, C, ?, DF>>
	extends TransferDelegate<T, S, V, C, DF, E> {
	protected final T object;
	protected final ExportTarget exportTarget;
	protected final String label;
	protected final String batchId;
	protected final boolean batchHead;
	protected final String name;
	protected final Collection<CmfObjectRef> parentIds;
	protected final String subType;

	protected ExportDelegate(DF factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass);
		if (object == null) { throw new IllegalArgumentException("Must provide a source object to export"); }
		this.object = object;

		// Now we invoke everything that needs to be calculated
		this.exportTarget = new ExportTarget(calculateType(object), calculateObjectId(object),
			calculateSearchKey(object));
		this.label = calculateLabel(object);
		this.batchId = calculateBatchId(object);
		this.batchHead = calculateBatchHead(object);
		this.subType = calculateSubType(this.exportTarget.getType(), object);
		if (factory.getEngine().isSupportsDuplicateFileNames()) {
			// We only calculate parent IDs
			Collection<CmfObjectRef> parentIds = calculateParentIds(object);
			if (parentIds == null) {
				parentIds = Collections.emptySet();
			}
			this.parentIds = Tools.freezeList(new ArrayList<CmfObjectRef>(parentIds));
		} else {
			this.parentIds = Collections.emptyList();
		}
		this.name = calculateName(object);
		if (this.subType == null) { throw new IllegalStateException("calculateSubType() may not return null"); }
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

	protected abstract String calculateName(T object) throws Exception;

	public final String getName() {
		return this.name;
	}

	protected Collection<CmfObjectRef> calculateParentIds(T object) throws Exception {
		return null;
	}

	public final Collection<CmfObjectRef> getParentIds() {
		return this.parentIds;
	}

	protected String calculateBatchId(T object) throws Exception {
		return null;
	}

	public final String getBatchId() {
		return this.batchId;
	}

	protected boolean calculateBatchHead(T object) throws Exception {
		// Default to true...
		return true;
	}

	public final boolean getBatchHead() {
		return this.batchHead;
	}

	protected String calculateSubType(CmfType type, T object) throws Exception {
		return type.name();
	}

	public final String getSubType() {
		return this.subType;
	}

	protected abstract Collection<? extends ExportDelegate<?, S, W, V, C, DF, ?>> identifyRequirements(
		CmfObject<V> marshalled, C ctx) throws Exception;

	protected void requirementsExported(CmfObject<V> marshalled, C ctx) throws ExportException {
	}

	protected Collection<? extends ExportDelegate<?, S, W, V, C, DF, ?>> identifyAntecedents(CmfObject<V> marshalled,
		C ctx) throws Exception {
		return Collections.emptyList();
	}

	protected void antecedentsExported(CmfObject<V> marshalled, C ctx) throws ExportException {
	}

	final CmfObject<V> marshal(C ctx, ExportTarget referrent) throws ExportException {
		CmfObject<V> marshaled = new CmfObject<V>(this.factory.getTranslator(), this.exportTarget.getType(),
			this.exportTarget.getId(), this.name, this.parentIds, this.exportTarget.getSearchKey(), this.batchId,
			this.batchHead, this.label, this.subType, ctx.getProductName(), ctx.getProductVersion(), null);
		if (!marshal(ctx, marshaled)) { return null; }
		this.factory.getEngine().setReferrent(marshaled, referrent);
		return marshaled;
	}

	protected void prepareForStorage(C ctx, CmfObject<V> object) throws ExportException {
		// By default, do nothing.
	}

	protected Collection<? extends ExportDelegate<?, S, W, V, C, DF, ?>> identifySuccessors(CmfObject<V> marshalled,
		C ctx) throws Exception {
		return Collections.emptyList();
	}

	protected void successorsExported(CmfObject<V> marshalled, C ctx) throws ExportException {
	}

	protected abstract boolean marshal(C ctx, CmfObject<V> object) throws ExportException;

	protected abstract List<CmfContentInfo> storeContent(C ctx, CmfAttributeTranslator<V> translator,
		CmfObject<V> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore) throws Exception;

	protected abstract Collection<? extends ExportDelegate<?, S, W, V, C, DF, ?>> identifyDependents(
		CmfObject<V> marshalled, C ctx) throws Exception;

	protected void dependentsExported(CmfObject<V> marshalled, C ctx) throws ExportException {
	}
}