package com.armedia.caliente.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferDelegate;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.Tools;

public abstract class ExportDelegate< //
	T, //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	EXPORT_CONTEXT extends ExportContext<SESSION, VALUE, ?>, //
	EXPORT_DELEGATE_FACTORY extends ExportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, EXPORT_ENGINE>, //
	EXPORT_ENGINE extends ExportEngine<SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, ?, EXPORT_DELEGATE_FACTORY> //
> extends TransferDelegate<T, SESSION, VALUE, EXPORT_CONTEXT, EXPORT_DELEGATE_FACTORY, EXPORT_ENGINE> {
	protected final T object;
	protected final ExportTarget exportTarget;
	protected final String label;
	protected final int dependencyTier;
	protected final String historyId;
	protected final boolean historyCurrent;
	protected final String name;
	protected final Collection<CmfObjectRef> parentIds;
	protected final String subType;
	protected final Set<String> secondaries;

	protected ExportDelegate(EXPORT_DELEGATE_FACTORY factory, SESSION session, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass);
		if (object == null) { throw new IllegalArgumentException("Must provide a source object to export"); }
		this.object = object;

		// Now we invoke everything that needs to be calculated
		this.exportTarget = new ExportTarget(calculateType(session, object), calculateObjectId(session, object),
			calculateSearchKey(session, object));
		this.label = calculateLabel(session, object);
		this.dependencyTier = calculateDependencyTier(session, object);
		this.historyId = calculateHistoryId(session, object);
		this.historyCurrent = calculateHistoryCurrent(session, object);
		this.subType = calculateSubType(session, this.exportTarget.getType(), object);
		this.secondaries = calculateSecondarySubtypes(session, this.exportTarget.getType(), this.subType, object);
		Collection<CmfObjectRef> parentIds = calculateParentIds(session, object);
		if (parentIds == null) {
			parentIds = Collections.emptySet();
		}
		this.parentIds = Tools.freezeList(new ArrayList<>(parentIds));
		this.name = calculateName(session, object);
		if (this.subType == null) { throw new IllegalStateException("calculateSubType() may not return null"); }
	}

	public final ExportTarget getExportTarget() {
		return this.exportTarget;
	}

	protected abstract CmfType calculateType(SESSION session, T object) throws Exception;

	public final CmfType getType() {
		return this.exportTarget.getType();
	}

	protected abstract String calculateLabel(SESSION session, T object) throws Exception;

	public final String getLabel() {
		return this.label;
	}

	protected abstract String calculateObjectId(SESSION session, T object) throws Exception;

	public final String getObjectId() {
		return this.exportTarget.getId();
	}

	protected abstract String calculateSearchKey(SESSION session, T object) throws Exception;

	public final String getSearchKey() {
		return this.exportTarget.getSearchKey();
	}

	protected abstract String calculateName(SESSION session, T object) throws Exception;

	public final String getName() {
		return this.name;
	}

	protected Collection<CmfObjectRef> calculateParentIds(SESSION session, T object) throws Exception {
		return null;
	}

	public final Collection<CmfObjectRef> getParentIds() {
		return this.parentIds;
	}

	protected int calculateDependencyTier(SESSION session, T object) throws Exception {
		return 0;
	}

	protected String calculateHistoryId(SESSION session, T object) throws Exception {
		return null;
	}

	public final int getDependencyTier() {
		return this.dependencyTier;
	}

	public final String getHistoryId() {
		return this.historyId;
	}

	protected boolean calculateHistoryCurrent(SESSION session, T object) throws Exception {
		// Default to true...
		return true;
	}

	public final boolean isHistoryCurrent() {
		return this.historyCurrent;
	}

	protected String calculateSubType(SESSION session, CmfType type, T object) throws Exception {
		return type.name();
	}

	protected Set<String> calculateSecondarySubtypes(SESSION session, CmfType type, String subtype, T object)
		throws Exception {
		return new LinkedHashSet<>();
	}

	public final String getSubType() {
		return this.subType;
	}

	protected abstract Collection<? extends ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, EXPORT_DELEGATE_FACTORY, ?>> identifyRequirements(
		CmfObject<VALUE> marshalled, EXPORT_CONTEXT ctx) throws Exception;

	protected void requirementsExported(CmfObject<VALUE> marshalled, EXPORT_CONTEXT ctx) throws Exception {
	}

	protected Collection<? extends ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, EXPORT_DELEGATE_FACTORY, ?>> identifyAntecedents(CmfObject<VALUE> marshalled,
		EXPORT_CONTEXT ctx) throws Exception {
		return new ArrayList<>();
	}

	protected void antecedentsExported(CmfObject<VALUE> marshalled, EXPORT_CONTEXT ctx) throws Exception {
	}

	final CmfObject<VALUE> marshal(EXPORT_CONTEXT ctx, ExportTarget referrent) throws ExportException {
		CmfObject<VALUE> marshaled = new CmfObject<>(this.factory.getTranslator(), this.exportTarget.getType(),
			this.exportTarget.getId(), this.name, this.parentIds, this.exportTarget.getSearchKey(), this.dependencyTier,
			this.historyId, this.historyCurrent, this.label, this.subType, this.secondaries, ctx.getProductName(),
			ctx.getProductVersion(), null);
		if (!marshal(ctx, marshaled)) { return null; }
		this.factory.getEngine().setReferrent(marshaled, referrent);
		return marshaled;
	}

	protected void prepareForStorage(EXPORT_CONTEXT ctx, CmfObject<VALUE> object) throws Exception {
		// By default, do nothing.
	}

	protected Collection<? extends ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, EXPORT_DELEGATE_FACTORY, ?>> identifySuccessors(CmfObject<VALUE> marshalled,
		EXPORT_CONTEXT ctx) throws Exception {
		return new ArrayList<>();
	}

	protected void successorsExported(CmfObject<VALUE> marshalled, EXPORT_CONTEXT ctx) throws Exception {
	}

	protected abstract boolean marshal(EXPORT_CONTEXT ctx, CmfObject<VALUE> object) throws ExportException;

	protected abstract List<CmfContentStream> storeContent(EXPORT_CONTEXT ctx, CmfAttributeTranslator<VALUE> translator,
		CmfObject<VALUE> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception;

	protected abstract Collection<? extends ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, EXPORT_DELEGATE_FACTORY, ?>> identifyDependents(
		CmfObject<VALUE> marshalled, EXPORT_CONTEXT ctx) throws Exception;

	protected void dependentsExported(CmfObject<VALUE> marshalled, EXPORT_CONTEXT ctx) throws Exception {
	}
}