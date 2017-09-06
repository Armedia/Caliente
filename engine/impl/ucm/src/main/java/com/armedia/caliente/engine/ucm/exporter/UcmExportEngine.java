package com.armedia.caliente.engine.ucm.exporter;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.UcmCommon;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionFactory;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.engine.ucm.UcmTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class UcmExportEngine extends
	ExportEngine<UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportContextFactory, UcmExportDelegateFactory> {

	public UcmExportEngine() {
		super(new CmfCrypt());
	}

	protected ExportTarget newExportTarget(QueryResult r) throws ExportException {
		return null;
	}

	@Override
	protected void findExportResults(final UcmSession session, CfgTools cfg, UcmExportDelegateFactory factory,
		TargetSubmitter submitter) throws Exception {
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected UcmSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new UcmSessionFactory(cfg, crypto);
	}

	@Override
	protected UcmExportContextFactory newContextFactory(UcmSession session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, CmfTypeMapper typeMapper, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new UcmExportContextFactory(this, session, cfg, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected UcmExportDelegateFactory newDelegateFactory(UcmSession session, CfgTools cfg) throws Exception {
		return new UcmExportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return UcmCommon.TARGETS;
	}

	public static ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(UcmCommon.TARGET_NAME);
	}

	@Override
	protected UcmTranslator getTranslator() {
		return new UcmTranslator();
	}
}