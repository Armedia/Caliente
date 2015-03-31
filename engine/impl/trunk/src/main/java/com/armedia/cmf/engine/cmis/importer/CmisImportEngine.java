package com.armedia.cmf.engine.cmis.importer;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Property;
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
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportEngine extends
ImportEngine<Session, CmisSessionWrapper, CmisObject, Property<?>, CmisImportContext> {

	public CmisImportEngine() {
	}

	@Override
	protected ImportStrategy getImportStrategy(StoredObjectType type) {
		return null;
	}

	@Override
	protected ImportOutcome importObject(StoredObject<?> marshaled,
		ObjectStorageTranslator<CmisObject, Property<?>> translator, CmisImportContext ctx) throws ImportException,
		StorageException, StoredValueDecoderException {
		return null;
	}

	@Override
	protected Property<?> getValue(StoredDataType type, Object value) {
		return null;
	}

	@Override
	protected ObjectStorageTranslator<CmisObject, Property<?>> getTranslator() {
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
	protected Set<String> getTargetNames() {
		return CmisCommon.TARGETS;
	}
}