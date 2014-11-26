/**
 *
 */

package com.armedia.cmf.engine.sharepoint.importer;

import java.util.Set;

import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.engine.sharepoint.ShptObject;
import com.armedia.cmf.engine.sharepoint.ShptSessionFactory;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptImportEngine<V> extends
ImportEngine<Service, ShptSessionWrapper, ShptObject<?>, V, ShptImportContext<V>> {

	@Override
	protected ImportStrategy getImportStrategy(StoredObjectType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ImportOutcome importObject(StoredObject<?> marshaled,
		ObjectStorageTranslator<ShptObject<?>, V> translator, ShptImportContext<V> ctx) throws ImportException,
		StorageException, StoredValueDecoderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected V getValue(StoredDataType type, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ObjectStorageTranslator<ShptObject<?>, V> getTranslator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ShptSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new ShptSessionFactory(cfg);
	}

	@Override
	protected ShptImportContextFactory<V> newContextFactory(CfgTools cfg) throws Exception {
		return new ShptImportContextFactory<V>(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		// TODO Auto-generated method stub
		return null;
	}
}