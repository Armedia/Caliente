package com.armedia.cmf.documentum.engine.importer;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

public class DctmImportFolder extends DctmImportAbstract<IDfFolder> {

	protected DctmImportFolder(DctmImportEngine engine) {
		super(engine, DctmObjectType.FOLDER);
	}

	@Override
	protected ImportOutcome importObject(StoredObject<?> marshaled,
		ObjectStorageTranslator<IDfPersistentObject, IDfValue> translator, DctmImportContext ctx)
			throws ImportException, StorageException, StoredValueDecoderException, DfException {
		return null;
	}
}