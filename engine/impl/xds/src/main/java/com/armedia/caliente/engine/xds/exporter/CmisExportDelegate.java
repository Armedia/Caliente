package com.armedia.caliente.engine.xds.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.xds.CmisSessionWrapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public abstract class CmisExportDelegate<T> extends
	ExportDelegate<T, Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportDelegateFactory, CmisExportEngine> {

	protected CmisExportDelegate(CmisExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected List<CmfContentInfo> storeContent(CmisExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		return new ArrayList<>();
	}
}