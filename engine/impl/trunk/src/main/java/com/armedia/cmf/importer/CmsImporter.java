/**
 *
 */

package com.armedia.cmf.importer;

import com.armedia.cmf.storage.CmsObject;
import com.armedia.cmf.storage.CmsObjectStore;
import com.armedia.cmf.storage.CmsStorageException;

/**
 * @author diego
 *
 */
public interface CmsImporter<V, D extends CmsDecoder<V>> {

	public void importObject(CmsObject object, CmsObjectStore objectStore, D decoder) throws CmsImportException,
		CmsStorageException;

}