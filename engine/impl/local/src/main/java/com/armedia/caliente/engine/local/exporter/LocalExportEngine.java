package com.armedia.caliente.engine.local.exporter;

import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.CmfCrypt;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionFactory;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.engine.local.common.LocalTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportEngine extends
	ExportEngine<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportContextFactory, LocalExportDelegateFactory> {

	public LocalExportEngine() {
		super(new CmfCrypt());
	}

	@Override
	protected void findExportResults(LocalRoot session, CfgTools configuration, LocalExportDelegateFactory factory,
		TargetSubmitter submitter) throws Exception {
		Iterator<ExportTarget> it = new LocalRecursiveIterator(session,
			configuration.getBoolean(LocalSetting.IGNORE_EMPTY_FOLDERS));
		while (it.hasNext()) {
			submitter.submit(it.next());
		}
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
	protected LocalExportContextFactory newContextFactory(LocalRoot session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, CmfTypeMapper typeMapper, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new LocalExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected LocalExportDelegateFactory newDelegateFactory(LocalRoot session, CfgTools cfg) throws Exception {
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