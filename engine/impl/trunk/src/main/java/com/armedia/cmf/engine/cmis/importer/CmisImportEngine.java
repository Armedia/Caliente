package com.armedia.cmf.engine.cmis.importer;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisCommon;
import com.armedia.cmf.engine.cmis.CmisSessionFactory;
import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportEngine extends
	ImportEngine<Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportDelegateFactory> {

	public CmisImportEngine() {
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
		return null;
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return null;
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return null;
	}

	@Override
	protected CmisSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new CmisSessionFactory(cfg);
	}

	@Override
	protected CmisImportContextFactory newContextFactory(CfgTools cfg) throws Exception {
		return new CmisImportContextFactory(this, cfg);
	}

	@Override
	protected CmisImportDelegateFactory newDelegateFactory(CfgTools cfg) throws Exception {
		return new CmisImportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return CmisCommon.TARGETS;
	}

	public static ImportEngine<?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(CmisCommon.TARGET_NAME);
	}
}