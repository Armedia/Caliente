package com.armedia.caliente.engine.local.exporter;

import java.util.Set;

import com.armedia.caliente.engine.CmfCrypt;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionFactory;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.CloseableIteratorWrapper;

public class LocalExportEngine extends
	ExportEngine<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportContextFactory, LocalExportDelegateFactory> {

	public LocalExportEngine() {
		super(new CmfCrypt());
	}

	@Override
	protected CloseableIterator<ExportTarget> findExportResults(LocalRoot session, CfgTools configuration,
		LocalExportDelegateFactory factory) throws Exception {
		return new CloseableIteratorWrapper<>(new LocalRecursiveIterator(session, true));
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new LocalTranslator();
	}

	@Override
	protected LocalSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new LocalSessionFactory(cfg, crypto);
	}

	@Override
	protected LocalExportContextFactory newContextFactory(LocalRoot root, CfgTools cfg) throws Exception {
		return new LocalExportContextFactory(this, cfg, root);
	}

	@Override
	protected LocalExportDelegateFactory newDelegateFactory(LocalRoot root, CfgTools cfg) throws Exception {
		return new LocalExportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return LocalCommon.TARGETS;
	}

	public static ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(LocalCommon.TARGET_NAME);
	}
}