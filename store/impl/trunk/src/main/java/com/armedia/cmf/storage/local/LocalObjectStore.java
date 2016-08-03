/**
 *
 */

package com.armedia.cmf.storage.local;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectHandler;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;

/**
 * @author diego
 *
 */
public class LocalObjectStore extends CmfObjectStore<File, LocalStoreOperation> {

	/**
	 * @throws CmfStorageException
	 */
	public LocalObjectStore() throws CmfStorageException {
		super(LocalStoreOperation.class);
	}

	/**
	 * @param openState
	 * @throws CmfStorageException
	 */
	public LocalObjectStore(boolean openState) throws CmfStorageException {
		super(LocalStoreOperation.class, openState);
	}

	@Override
	protected LocalStoreOperation newOperation() throws CmfStorageException {
		return null;
	}

	@Override
	protected <V> Long storeObject(LocalStoreOperation operation, CmfObject<V> object,
		CmfAttributeTranslator<V> translator) throws CmfStorageException {
		return null;
	}

	@Override
	protected <V> void setContentInfo(LocalStoreOperation operation, CmfObject<V> object,
		Collection<CmfContentInfo> content) throws CmfStorageException {
	}

	@Override
	protected <V> List<CmfContentInfo> getContentInfo(LocalStoreOperation operation, CmfObject<V> object)
		throws CmfStorageException {
		return null;
	}

	@Override
	protected boolean isStored(LocalStoreOperation operation, CmfType type, String objectId)
		throws CmfStorageException {
		return false;
	}

	@Override
	protected boolean lockForStorage(LocalStoreOperation operation, CmfType type, String objectId)
		throws CmfStorageException {
		// TODO: Use a memory-based H2 DB? Possibly with swap?
		return false;
	}

	@Override
	protected <V> int loadObjects(LocalStoreOperation operation, CmfAttributeTranslator<V> translator,
		final CmfType type, Collection<String> ids, CmfObjectHandler<V> handler, boolean batching)
		throws CmfStorageException {
		return 0;
	}

	@Override
	protected void createMapping(LocalStoreOperation operation, CmfType type, String name, String source, String target)
		throws CmfStorageException {
		// TODO: Use a memory-based H2 DB? Possibly with swap?
	}

	@Override
	protected String getMapping(LocalStoreOperation operation, boolean source, CmfType type, String name, String value)
		throws CmfStorageException {
		// TODO: Use a memory-based H2 DB? Possibly with swap?
		return null;
	}

	@Override
	protected Map<CmfType, Integer> getStoredObjectTypes(LocalStoreOperation operation) throws CmfStorageException {
		return null;
	}

	@Override
	protected int clearAttributeMappings(LocalStoreOperation operation) throws CmfStorageException {
		return 0;
	}

	@Override
	protected Map<CmfType, Set<String>> getAvailableMappings(LocalStoreOperation operation) throws CmfStorageException {
		return null;
	}

	@Override
	protected Set<String> getAvailableMappings(LocalStoreOperation operation, CmfType type) throws CmfStorageException {
		return null;
	}

	@Override
	protected Map<String, String> getMappings(LocalStoreOperation operation, CmfType type, String name)
		throws CmfStorageException {
		return null;
	}

	@Override
	protected void clearAllObjects(LocalStoreOperation operation) throws CmfStorageException {
	}

	@Override
	protected void clearProperties(LocalStoreOperation operation) throws CmfStorageException {
	}

	@Override
	protected CmfValue getProperty(LocalStoreOperation operation, String property) throws CmfStorageException {
		return null;
	}

	@Override
	protected CmfValue setProperty(LocalStoreOperation operation, String property, CmfValue value)
		throws CmfStorageException {
		return null;
	}

	@Override
	protected Set<String> getPropertyNames(LocalStoreOperation operation) throws CmfStorageException {
		return null;
	}

	@Override
	protected CmfValue clearProperty(LocalStoreOperation operation, String property) throws CmfStorageException {
		return null;
	}

}