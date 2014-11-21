/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import java.util.Collections;
import java.util.Set;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DctmSessionFactory;
import com.armedia.cmf.documentum.engine.DctmSessionWrapper;
import com.armedia.cmf.documentum.engine.DctmTranslator;
import com.armedia.cmf.documentum.engine.DfValueFactory;
import com.armedia.cmf.documentum.engine.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.SessionFactory;
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
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportEngine extends
	ImportEngine<IDfSession, DctmSessionWrapper, IDfPersistentObject, IDfValue, DctmImportContext> {

	private static final ImportStrategy NOT_SUPPORTED = new ImportStrategy() {
		@Override
		public boolean isIgnored() {
			return true;
		}

		@Override
		public BatchingStrategy getBatchingStrategy() {
			return null;
		}

		@Override
		public boolean isParallelCapable() {
			return true;
		}

		@Override
		public boolean isBatchFailRemainder() {
			return true;
		}
	};

	private static final Set<String> TARGETS = Collections.singleton("dctm");

	public DctmImportEngine() {
	}

	private DctmImportDelegate<?> getImportDelegate(StoredObject<IDfValue> marshaled)
		throws UnsupportedDctmObjectTypeException, UnsupportedObjectTypeException {
		return DctmImportDelegateFactory.newDelegate(this, marshaled);
	}

	@Override
	protected ImportStrategy getImportStrategy(StoredObjectType type) {
		DctmObjectType dctmType = DctmTranslator.translateType(type);
		if (dctmType == null) { return DctmImportEngine.NOT_SUPPORTED; }
		return dctmType.importStrategy;
	}

	@Override
	protected ImportOutcome importObject(StoredObject<?> marshaled,
		ObjectStorageTranslator<IDfPersistentObject, IDfValue> translator, DctmImportContext ctx)
			throws ImportException, StorageException, StoredValueDecoderException {
		@SuppressWarnings("unchecked")
		StoredObject<IDfValue> castedMarshaled = (StoredObject<IDfValue>) marshaled;
		try {
			return getImportDelegate(castedMarshaled).importObject(ctx);
		} catch (DfException | UnsupportedDctmObjectTypeException | UnsupportedObjectTypeException e) {
			throw new ImportException(String.format("Exception raised while marshaling %s [%s](%s)",
				marshaled.getType(), marshaled.getLabel(), marshaled.getId()), e);
		}
	}

	@Override
	protected DctmImportContextFactory newContextFactory(CfgTools config) {
		return new DctmImportContextFactory(this, config);
	}

	@Override
	protected IDfValue getValue(StoredDataType type, Object value) {
		return DfValueFactory.newValue(type, value);
	}

	@Override
	protected ObjectStorageTranslator<IDfPersistentObject, IDfValue> getTranslator() {
		return DctmTranslator.INSTANCE;
	}

	@Override
	protected SessionFactory<IDfSession> newSessionFactory(CfgTools config) throws Exception {
		return new DctmSessionFactory(config);
	}

	@Override
	protected Set<String> getTargetNames() {
		return DctmImportEngine.TARGETS;
	}

	@Override
	protected boolean abortImport(StoredObjectType type, int errors) {
		if (type == StoredObjectType.DATASTORE) {
			// We MUST have all datastores present
			return (errors == 0);
		}
		return super.abortImport(type, errors);
	}
}