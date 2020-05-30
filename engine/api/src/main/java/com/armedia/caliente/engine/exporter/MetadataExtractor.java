package com.armedia.caliente.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;

public abstract class MetadataExtractor<VALUE, CONTEXT extends ExportContext<?, VALUE, ?>> implements AutoCloseable {

	protected class ExportTargetBundle<T> {
		private final ExportTarget exportTarget;
		private final T bundled;

		public ExportTargetBundle(ExportTarget exportTarget, T bundled) {
			this.exportTarget = exportTarget;
			this.bundled = bundled;
		}

		public ExportTarget getExportTarget() {
			return this.exportTarget;
		}

		public T getBundled() {
			return this.bundled;
		}
	}

	protected abstract Collection<ExportTarget> identifyRequirements(CmfObject<VALUE> marshalled, CONTEXT ctx)
		throws Exception;

	protected Collection<ExportTarget> identifyAntecedents(CmfObject<VALUE> marshalled, CONTEXT ctx) throws Exception {
		return new ArrayList<>();
	}

	/*-
	final CmfObject<VALUE> marshal(CONTEXT ctx, ExportTarget referrent) throws ExportException {
		CmfObject<VALUE> marshaled = new CmfObject<>(this.factory.getTranslator(), this.exportTarget.getType(),
			this.exportTarget.getId(), this.name, this.parentIds, this.exportTarget.getSearchKey(), this.dependencyTier,
			this.historyId, this.historyCurrent, this.label, this.subType, this.secondaries, null);
		if (!marshal(ctx, marshaled)) { return null; }
		this.factory.getEngine().setReferrent(marshaled, referrent);
		return marshaled;
	}
	*/

	protected void prepareForStorage(CONTEXT ctx, CmfObject<VALUE> object) throws Exception {
		// By default, do nothing.
	}

	protected Collection<ExportTarget> identifySuccessors(CmfObject<VALUE> marshalled, CONTEXT ctx) throws Exception {
		return new ArrayList<>();
	}

	protected void successorsExported(CmfObject<VALUE> marshalled, CONTEXT ctx) throws Exception {
	}

	protected abstract boolean marshal(CONTEXT ctx, CmfObject<VALUE> object) throws ExportException;

	protected abstract List<CmfContentStream> storeContent(CONTEXT ctx, CmfAttributeTranslator<VALUE> translator,
		CmfObject<VALUE> marshalled, CmfContentStore<?, ?> streamStore, boolean includeRenditions);

	/*-
	protected abstract Collection<? extends ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, DELEGATE_FACTORY, ?>> identifyDependents(
		CmfObject<VALUE> marshalled, CONTEXT ctx) throws Exception;
	*/

	protected void dependentsExported(CmfObject<VALUE> marshalled, CONTEXT ctx) throws Exception {
	}
}