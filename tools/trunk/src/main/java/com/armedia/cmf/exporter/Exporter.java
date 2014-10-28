/**
 *
 */

package com.armedia.cmf.exporter;

import java.util.Map;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;

/**
 * @author diego
 *
 */
public interface Exporter<S, V> {

	public void runExport(ObjectStore objectStore, ObjectStorageTranslator<V> translator, Map<String, Object> settings)
		throws ExportException, StorageException;

}