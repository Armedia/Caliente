package com.armedia.cmf.engine.cmis.importer;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisCommon;
import com.armedia.cmf.engine.cmis.CmisSessionFactory;
import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportEngine extends ImportEngine<Session, CmisSessionWrapper, StoredValue, CmisImportContext> {

	public CmisImportEngine() {
	}

	@Override
	protected ImportStrategy getImportStrategy(StoredObjectType type) {
		return null;
	}

	@Override
	protected ImportOutcome importObject(StoredObject<?> marshaled, ObjectStorageTranslator<StoredValue> translator,
		CmisImportContext ctx) throws ImportException, StorageException, StoredValueDecoderException {
		return null;
	}

	@Override
	protected StoredValue getValue(StoredDataType type, Object value) {
		return null;
	}

	@Override
	protected ObjectStorageTranslator<StoredValue> getTranslator() {
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

	public static ImportEngine<?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(CmisCommon.TARGET_NAME);
	}
}