package com.armedia.caliente.engine.local.exporter;

import java.util.List;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

abstract class LocalExportDelegate<T> extends
	ExportDelegate<T, LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportDelegateFactory, LocalExportEngine> {

	protected final LocalRoot root;

	protected LocalExportDelegate(LocalExportDelegateFactory factory, LocalRoot root, Class<T> klass, T object)
		throws Exception {
		super(factory, root, klass, object);
		this.root = root;
	}

	@Override
	protected boolean calculateHistoryCurrent(LocalRoot root, T object) throws Exception {
		// Always true
		return true;
	}

	@Override
	protected List<CmfContentStream> storeContent(LocalExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?> streamStore,
		boolean includeRenditions) {
		return null;
	}
}