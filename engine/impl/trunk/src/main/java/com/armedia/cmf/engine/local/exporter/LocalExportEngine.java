package com.armedia.cmf.engine.local.exporter;

import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionFactory;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.LocalTranslator;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportEngine
	extends
	ExportEngine<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportContextFactory, LocalExportDelegateFactory> {

	@Override
	protected Iterator<ExportTarget> findExportResults(LocalRoot session, CfgTools configuration,
		LocalExportDelegateFactory factory) throws Exception {
		return new LocalRecursiveIterator(session, true);
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		try {
			return new CmfValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException(String.format("Can't convert [%s] as a %s", value, type), e);
		}
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new LocalTranslator();
	}

	@Override
	protected LocalSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new LocalSessionFactory(cfg);
	}

	@Override
	protected LocalExportContextFactory newContextFactory(LocalRoot root, CfgTools cfg) throws Exception {
		return new LocalExportContextFactory(this, cfg);
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