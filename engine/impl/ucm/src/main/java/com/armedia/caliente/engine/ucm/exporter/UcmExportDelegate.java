package com.armedia.caliente.engine.ucm.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public abstract class UcmExportDelegate<T> extends
	ExportDelegate<T, IdcSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportDelegateFactory, UcmExportEngine> {

	protected UcmExportDelegate(UcmExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
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
	protected List<CmfContentInfo> storeContent(UcmExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		return new ArrayList<>();
	}
}