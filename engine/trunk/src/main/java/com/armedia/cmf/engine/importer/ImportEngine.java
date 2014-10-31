/**
 *
 */

package com.armedia.cmf.engine.importer;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValueDecoderException;

/**
 * @author diego
 *
 */
public interface ImportEngine<S, T, V> {

	public void importObject(StoredObject<?> object, ObjectStore<?, ?> objectStore,
		ObjectStorageTranslator<T, V> translator) throws ImportException, StorageException, StoredValueDecoderException;

}