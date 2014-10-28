/**
 *
 */

package com.armedia.cmf.importer;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValueDecoderException;

/**
 * @author diego
 *
 */
public interface Importer<S, V> {

	public void importObject(StoredObject<?> object, ObjectStore objectStore, ObjectStorageTranslator<V> translator)
		throws ImportException, StorageException, StoredValueDecoderException;

}