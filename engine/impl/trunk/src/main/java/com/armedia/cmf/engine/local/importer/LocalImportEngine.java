package com.armedia.cmf.engine.local.importer;

import java.util.Set;

import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalSessionFactory;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportEngine extends ImportEngine<LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext> {

	@Override
	protected ImportStrategy getImportStrategy(StoredObjectType type) {
		switch (type) {
			case ACL:
			case DOCUMENT:
			case FOLDER:
				break;
			default:
				return null;
		}
		return null;
	}

	@Override
	protected ImportOutcome importObject(StoredObject<?> marshaled, ObjectStorageTranslator<StoredValue> translator,
		LocalImportContext ctx) throws ImportException, StorageException, StoredValueDecoderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StoredValue getValue(StoredDataType type, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ObjectStorageTranslator<StoredValue> getTranslator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected LocalSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new LocalSessionFactory(cfg);
	}

	@Override
	protected LocalImportContextFactory newContextFactory(CfgTools cfg) throws Exception {
		return new LocalImportContextFactory(this, cfg);
	}

	@Override
	protected LocalImportDelegateFactory newDelegateFactory(CfgTools cfg) throws Exception {
		return new LocalImportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return LocalCommon.TARGETS;
	}
}