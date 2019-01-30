package com.armedia.caliente.engine.ucm.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.engine.ucm.model.UcmModelObject;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public abstract class UcmExportDelegate<T extends UcmModelObject> extends
	ExportDelegate<T, UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportDelegateFactory, UcmExportEngine> {

	protected UcmExportDelegate(UcmExportDelegateFactory factory, UcmSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		UcmExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected List<CmfContentStream> storeContent(UcmExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		return new ArrayList<>();
	}
}