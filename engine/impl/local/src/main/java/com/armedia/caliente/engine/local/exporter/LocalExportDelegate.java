package com.armedia.caliente.engine.local.exporter;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.store.CmfValue;

abstract class LocalExportDelegate<T> extends
	ExportDelegate<T, LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportDelegateFactory, LocalExportEngine> {

	protected LocalExportDelegate(LocalExportDelegateFactory factory, LocalRoot root, Class<T> klass, T object)
		throws Exception {
		super(factory, root, klass, object);
	}

	@Override
	protected boolean calculateHistoryCurrent(LocalRoot root, T object) throws Exception {
		// Always true
		return true;
	}
}